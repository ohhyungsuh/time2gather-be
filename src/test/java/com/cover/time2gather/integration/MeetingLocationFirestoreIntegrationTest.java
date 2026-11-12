package com.cover.time2gather.integration;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.*;
import com.cover.time2gather.domain.meeting.service.MeetingLocationService;
import com.cover.time2gather.infra.meeting.*;
import com.cover.time2gather.support.FirestoreIntegrationTest;
import com.cover.time2gather.infra.firestore.FirestoreMeetingLocationSelectionRepository;
import com.google.cloud.firestore.*;
import org.mockito.AdditionalAnswers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MeetingLocationFirestoreIntegrationTest extends FirestoreIntegrationTest {
    @Autowired MeetingLocationService service;
    @Autowired MeetingRepository meetings;
    @Autowired MeetingLocationRepository locations;
    @Autowired MeetingLocationSelectionRepository votes;
    @Autowired Firestore db;
    Meeting meeting;
    MeetingLocation a, b, c;

    @BeforeEach void createMeeting() {
        meeting = meetings.save(Meeting.create("mtg_" + UUID.randomUUID().toString().replace("-", ""),
                "Location integration", "", 1L, "UTC", SelectionType.TIME, 60,
                Map.of("2027-01-04", new int[]{9, 10}), true));
        a = locations.save(MeetingLocation.create(meeting.getId(), "A", 0));
        b = locations.save(MeetingLocation.create(meeting.getId(), "B", 1));
        c = locations.save(MeetingLocation.create(meeting.getId(), "C", 2));
        service.voteLocations(meeting.getMeetingCode(), 2L, List.of(a.getId()));
    }

    List<Long> saved() { return service.selectUserLocationIds(meeting.getMeetingCode(), 2L); }

    @Test void rejectedInvalidLocationPreservesPreviousVote() {
        assertThatThrownBy(() -> service.voteLocations(meeting.getMeetingCode(), 2L, List.of(Long.MAX_VALUE)))
                .isInstanceOf(BusinessException.class);
        assertThat(saved()).containsExactly(a.getId());
    }

    @Test void mixedValidAndInvalidLocationsNeverPartiallyReplaceVote() {
        assertThatThrownBy(() -> service.voteLocations(meeting.getMeetingCode(), 2L, List.of(b.getId(), Long.MAX_VALUE)))
                .isInstanceOf(BusinessException.class);
        assertThat(saved()).containsExactly(a.getId());
    }

    @Test void staleCandidateRemovedByHostDoesNotErasePreviousVote() {
        service.deleteLocation(meeting.getMeetingCode(), 1L, c.getId());
        assertThatThrownBy(() -> service.voteLocations(meeting.getMeetingCode(), 2L, List.of(c.getId())))
                .isInstanceOf(BusinessException.class);
        assertThat(saved()).containsExactly(a.getId());
    }

    @Test void duplicateLocationIdsAndRetriesDoNotDuplicateVotes() {
        service.voteLocations(meeting.getMeetingCode(), 2L, List.of(b.getId(), b.getId()));
        assertThat(saved()).containsExactly(b.getId());
        service.voteLocations(meeting.getMeetingCode(), 2L, List.of(b.getId(), b.getId()));
        assertThat(votes.selectByMeetingIdAndUserId(meeting.getId(), 2L)).hasSize(1);
    }

    @Test void clearAndLegacyDuplicateReplacementKeepTheExactRequestedSet() {
        votes.save(MeetingLocationSelection.create(meeting.getId(), a.getId(), 2L));
        assertThat(votes.selectByMeetingIdAndUserId(meeting.getId(), 2L)).hasSize(2);
        service.voteLocations(meeting.getMeetingCode(), 2L, List.of(b.getId(), c.getId()));
        assertThat(saved()).containsExactlyInAnyOrder(b.getId(), c.getId());
        assertThat(votes.selectByLocationId(a.getId())).isEmpty();
        assertThat(votes.countByLocationId(b.getId())).isEqualTo(1);
        assertThat(votes.existsByMeetingIdAndLocationIdAndUserId(meeting.getId(), b.getId(), 2L)).isTrue();
        assertThat(votes.existsByMeetingIdAndLocationIdAndUserId(meeting.getId(), a.getId(), 2L)).isFalse();
        service.voteLocations(meeting.getMeetingCode(), 2L, null);
        assertThat(saved()).isEmpty();
        service.voteLocations(meeting.getMeetingCode(), 2L, List.of());
        assertThat(saved()).isEmpty();
    }

    @Test void replacementRevalidatesCandidatesAndTheMeetingBeforeAnyMutation() {
        var store = (FirestoreMeetingLocationSelectionRepository) votes;
        expectError(() -> store.replaceSelections(meeting.getId(), 2L, List.of(Long.MAX_VALUE)), ErrorCode.LOCATION_NOT_EXIST);
        expectError(() -> store.replaceSelections(Long.MAX_VALUE, 2L, List.of()), ErrorCode.MEETING_NOT_FOUND);
        var disabled = meetings.save(newMeeting(false));
        expectError(() -> store.replaceSelections(disabled.getId(), 2L, List.of()), ErrorCode.LOCATION_VOTE_NOT_ENABLED);
        var foreign = locations.save(MeetingLocation.create(disabled.getId(), "Foreign", 0));
        expectError(() -> store.replaceSelections(meeting.getId(), 2L, List.of(foreign.getId())), ErrorCode.LOCATION_NOT_EXIST);
        assertThat(saved()).containsExactly(a.getId());
    }

    @Test @SuppressWarnings({"unchecked", "rawtypes"})
    void failedFirestoreCommitPreservesVotesAndGuardTogether() throws Exception {
        var guard = db.collection("meetingLocationVoteGuards").document(meeting.getId() + "-2");
        long guardBefore = guard.get().get().getLong("version");
        Firestore failing = mock(Firestore.class, AdditionalAnswers.delegatesTo(db));
        CollectionReference wrapped = mock(CollectionReference.class,
                AdditionalAnswers.delegatesTo(db.collection("meetingLocationSelections")));
        doReturn(failing).when(wrapped).getFirestore();
        doReturn(wrapped).when(failing).collection("meetingLocationSelections");
        doAnswer(invocation -> {
            WriteBatch real = db.batch();
            WriteBatch proxy = mock(WriteBatch.class, AdditionalAnswers.delegatesTo(real));
            doAnswer(commit -> {
                real.update(db.collection("missingAuditDocuments").document("absent"), "value", 1);
                return real.commit();
            }).when(proxy).commit();
            return proxy;
        }).when(failing).batch();
        var store = new FirestoreMeetingLocationSelectionRepository(failing);
        assertThatThrownBy(() -> store.replaceSelections(meeting.getId(), 2L, List.of(b.getId())))
                .isInstanceOf(IllegalStateException.class);
        assertThat(saved()).containsExactly(a.getId());
        assertThat(guard.get().get().getLong("version")).isEqualTo(guardBefore);
    }

    @Test void simultaneousReplacementsForOnePersonNeverUnionCompetingChoices() throws Exception {
        together(8, index -> service.voteLocations(meeting.getMeetingCode(), 2L,
                List.of(index % 2 == 0 ? a.getId() : b.getId())));
        assertThat(saved()).hasSize(1).allMatch(id -> id.equals(a.getId()) || id.equals(b.getId()));
        assertThat(votes.selectByMeetingIdAndUserId(meeting.getId(), 2L)).hasSize(1);
    }

    @Test void concurrentDifferentPeopleNeverEraseEachOthersVotes() throws Exception {
        together(8, index -> service.voteLocations(meeting.getMeetingCode(), 100L + index, List.of(a.getId(), b.getId())));
        for (long user = 100; user < 108; user++) {
            assertThat(service.selectUserLocationIds(meeting.getMeetingCode(), user)).containsExactlyInAnyOrder(a.getId(), b.getId());
        }
        assertThat(saved()).containsExactly(a.getId());
        assertThat(votes.countByLocationId(a.getId())).isEqualTo(9);
        assertThat(votes.countByLocationId(b.getId())).isEqualTo(8);
    }

    @Test void hostCrudReordersLocationsAndRemovesOnlyDeletedLocationVotes() {
        var added = service.addLocation(meeting.getMeetingCode(), 1L, "D");
        service.voteLocations(meeting.getMeetingCode(), 3L, List.of(a.getId(), b.getId()));
        service.deleteLocation(meeting.getMeetingCode(), 1L, b.getId());
        var retained = service.selectLocationsByMeetingCode(meeting.getMeetingCode());
        assertThat(retained).extracting(MeetingLocation::getName).containsExactly("A", "C", "D");
        assertThat(retained).extracting(MeetingLocation::getDisplayOrder).containsExactly(0, 1, 2);
        assertThat(service.selectUserLocationIds(meeting.getMeetingCode(), 3L)).containsExactly(a.getId());
        assertThat(locations.findById(added.getId())).isPresent();
        assertThat(locations.countByMeetingId(meeting.getId())).isEqualTo(3);
        locations.deleteByMeetingId(meeting.getId());
        assertThat(locations.selectByMeetingId(meeting.getId())).isEmpty();
    }

    @Test void hostPermissionsAndLocationLimitsDoNotMutateTheExistingState() {
        expectError(() -> service.addLocation(meeting.getMeetingCode(), 99L, "D"), ErrorCode.MEETING_HOST_ONLY);
        expectError(() -> service.deleteLocation(meeting.getMeetingCode(), 99L, a.getId()), ErrorCode.MEETING_HOST_ONLY);
        expectError(() -> service.confirmLocation(meeting.getMeetingCode(), 99L, a.getId()), ErrorCode.MEETING_HOST_ONLY);
        expectError(() -> service.cancelLocationConfirmation(meeting.getMeetingCode(), 99L), ErrorCode.MEETING_HOST_ONLY);
        expectError(() -> service.addLocation(meeting.getMeetingCode(), 1L, null), ErrorCode.LOCATION_NAME_REQUIRED);
        expectError(() -> service.addLocation(meeting.getMeetingCode(), 1L, "  "), ErrorCode.LOCATION_NAME_REQUIRED);
        service.addLocation(meeting.getMeetingCode(), 1L, "D"); service.addLocation(meeting.getMeetingCode(), 1L, "E");
        expectError(() -> service.addLocation(meeting.getMeetingCode(), 1L, "F"), ErrorCode.LOCATION_MAX_EXCEEDED);
        assertThat(locations.countByMeetingId(meeting.getId())).isEqualTo(5);
        assertThat(saved()).containsExactly(a.getId());
    }

    @Test void missingForeignAndMinimumLocationConstraintsAreEnforced() {
        expectError(() -> service.addLocation("mtg_missing", 1L, "D"), ErrorCode.MEETING_NOT_FOUND);
        expectError(() -> service.deleteLocation(meeting.getMeetingCode(), 1L, Long.MAX_VALUE), ErrorCode.LOCATION_NOT_FOUND);
        var other = meetings.save(newMeeting(false));
        var foreign = locations.save(MeetingLocation.create(other.getId(), "Other", 0));
        expectError(() -> service.deleteLocation(meeting.getMeetingCode(), 1L, foreign.getId()), ErrorCode.LOCATION_NOT_BELONG_TO_MEETING);
        expectError(() -> service.addLocation(other.getMeetingCode(), 1L, "Disabled"), ErrorCode.LOCATION_VOTE_NOT_ENABLED);
        expectError(() -> service.selectUserLocationIds(other.getMeetingCode(), 2L), ErrorCode.LOCATION_VOTE_NOT_ENABLED);
        service.deleteLocation(meeting.getMeetingCode(), 1L, c.getId());
        expectError(() -> service.deleteLocation(meeting.getMeetingCode(), 1L, a.getId()), ErrorCode.LOCATION_MIN_REQUIRED);
        assertThat(saved()).containsExactly(a.getId());
    }

    @Test void locationConfirmationAndCancellationPersistAndEnforceTheirState() {
        expectError(() -> service.cancelLocationConfirmation(meeting.getMeetingCode(), 1L), ErrorCode.LOCATION_NOT_CONFIRMED);
        expectError(() -> service.confirmLocation(meeting.getMeetingCode(), 1L, Long.MAX_VALUE), ErrorCode.LOCATION_NOT_FOUND);
        var other = meetings.save(newMeeting(true));
        var foreign = locations.save(MeetingLocation.create(other.getId(), "Other", 0));
        expectError(() -> service.confirmLocation(meeting.getMeetingCode(), 1L, foreign.getId()), ErrorCode.LOCATION_NOT_BELONG_TO_MEETING);
        service.confirmLocation(meeting.getMeetingCode(), 1L, a.getId());
        assertThat(meetings.findById(meeting.getId()).orElseThrow().getConfirmedLocationId()).isEqualTo(a.getId());
        expectError(() -> service.confirmLocation(meeting.getMeetingCode(), 1L, b.getId()), ErrorCode.LOCATION_ALREADY_CONFIRMED);
        service.cancelLocationConfirmation(meeting.getMeetingCode(), 1L);
        assertThat(meetings.findById(meeting.getId()).orElseThrow().isLocationConfirmed()).isFalse();
    }

    Meeting newMeeting(boolean enabled) {
        return Meeting.create("mtg_" + UUID.randomUUID().toString().replace("-", ""), "Other", "", 1L,
                "UTC", SelectionType.TIME, 60, Map.of("2027-01-04", new int[]{9, 10}), enabled);
    }
    void expectError(Runnable operation, ErrorCode code) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(BusinessException.class,
                failure -> assertThat(failure.getErrorCode()).isEqualTo(code));
    }
    void together(int size, java.util.function.IntConsumer operation) throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(size)) {
            List<Future<?>> pending = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int index = i;
                pending.add(pool.submit(() -> { start.await(); operation.accept(index); return null; }));
            }
            start.countDown();
            for (var result : pending) result.get(45, TimeUnit.SECONDS);
        }
    }
}

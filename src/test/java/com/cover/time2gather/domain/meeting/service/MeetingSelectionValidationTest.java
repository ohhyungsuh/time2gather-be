package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.exception.*;
import com.cover.time2gather.domain.meeting.*;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.meeting.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingSelectionValidationTest {
    @Mock MeetingUserSelectionRepository selections;
    @Mock UserRepository users;
    @Mock MeetingRepository meetings;
    @InjectMocks MeetingSelectionService service;
    static final String DATE = "2027-01-04";

    Meeting meeting() {
        var meeting = Meeting.create("mtg_validation", "Validation", "", 1L, "UTC", SelectionType.TIME,
                60, Map.of(DATE, new int[]{9, 10}), false);
        ReflectionTestUtils.setField(meeting, "id", 1L);
        return meeting;
    }
    void existing(Meeting meeting) {
        when(users.existsById(2L)).thenReturn(true);
        when(meetings.findById(1L)).thenReturn(Optional.of(meeting));
    }
    void rejects(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                failure -> assertThat(failure.getErrorCode()).isEqualTo(code));
        verify(selections, never()).saveForCandidates(any(), any());
    }

    static Stream<Arguments> invalidSelections() {
        var nullSlots = new HashMap<String, int[]>(); nullSlots.put(DATE, null);
        return Stream.of(Arguments.of(Map.of("2027-01-05", new int[]{9}), ErrorCode.SELECTION_DATE_NOT_AVAILABLE),
                Arguments.of(Map.of(DATE, new int[0]), ErrorCode.SELECTION_SLOT_EMPTY_FOR_TIME),
                Arguments.of(nullSlots, ErrorCode.SELECTION_SLOT_NULL),
                Arguments.of(Map.of(DATE, new int[]{8}), ErrorCode.SELECTION_SLOT_NOT_AVAILABLE),
                Arguments.of(Map.of(), ErrorCode.MEETING_DATE_REQUIRED));
    }
    @ParameterizedTest @MethodSource("invalidSelections")
    void invalidTimeRequestsNeverWrite(Map<String, int[]> values, ErrorCode expected) {
        existing(meeting());
        rejects(() -> service.upsertUserSelections(1L, 2L, values), expected);
    }

    @Test void missingUserAndMissingMeetingAreRejectedBeforePersistence() {
        rejects(() -> service.upsertUserSelections(1L, 2L, Map.of(DATE, new int[]{9})), ErrorCode.USER_NOT_FOUND);
        when(users.existsById(2L)).thenReturn(true);
        rejects(() -> service.upsertUserSelections(1L, 2L, Map.of(DATE, new int[]{9})), ErrorCode.MEETING_NOT_FOUND);
    }

    @Test void updatingAnUnavailableResponseReplacesItsOldState() {
        var meeting = meeting(); existing(meeting);
        var previous = MeetingUserSelection.unavailable(meeting, 2L, List.of(DATE));
        when(selections.findByMeetingIdAndUserId(1L, 2L)).thenReturn(Optional.of(previous));
        service.upsertUserSelections(1L, 2L, Map.of(DATE, new int[]{10}));
        assertThat(previous.getUnavailableDates()).isNull();
        assertThat(previous.getSelections().get(DATE)).containsExactly(10);
        verify(selections).saveForCandidates(eq(previous), any());
    }

    @Test void readReturnsPersistedSelectionsOrEmptyForANewParticipant() {
        when(selections.findByMeetingIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        assertThat(service.getUserSelections(1L, 2L)).isEmpty();
        when(selections.findByMeetingIdAndUserId(1L, 2L)).thenReturn(Optional.of(
                MeetingUserSelection.create(1L, 2L, Map.of(DATE, new int[]{9}))));
        assertThat(service.getUserSelections(1L, 2L).get(DATE)).containsExactly(9);
    }

    @Test void unavailableResponseRequiresExistingActorAndMeetingAndRejectsConfirmation() {
        rejects(() -> service.markUnavailable(1L, 2L, List.of(DATE)), ErrorCode.USER_NOT_FOUND);
        when(users.existsById(2L)).thenReturn(true);
        rejects(() -> service.markUnavailable(1L, 2L, List.of(DATE)), ErrorCode.MEETING_NOT_FOUND);
        var confirmed = meeting(); confirmed.confirm(LocalDate.parse(DATE), 9);
        when(meetings.findById(1L)).thenReturn(Optional.of(confirmed));
        rejects(() -> service.markUnavailable(1L, 2L, List.of(DATE)), ErrorCode.MEETING_ALREADY_CONFIRMED);
    }

    @ParameterizedTest @NullSource @EmptySource @ValueSource(strings = {"2027-01-05"})
    void unavailableResponseRejectsMissingOrChangedCandidates(String date) {
        existing(meeting());
        List<String> candidates = date == null ? null : date.isEmpty() ? List.of() : List.of(date);
        rejects(() -> service.markUnavailable(1L, 2L, candidates), ErrorCode.SELECTION_CANDIDATES_CHANGED);
    }

    @Test void newUnavailableResponseIsStoredAndDoesNotInventAvailability() {
        existing(meeting());
        service.markUnavailable(1L, 2L, List.of(DATE));
        verify(selections).saveForCandidates(argThat(value -> value.getSelections().isEmpty()
                && value.getUnavailableDates().equals(List.of(DATE))), any());
    }
}

package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.meeting.*;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.Tag("firestore")
class FirestoreVoteConcurrencyTest {
    private Firestore db;
    private FirestoreMeetingUserSelectionRepository votes;
    private Meeting meeting;
    private static final String DATE = "2027-01-10";

    @BeforeEach void setUp() {
        db = FirestoreOptions.newBuilder().setProjectId("demo-vote-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .setEmulatorHost(com.cover.time2gather.support.FirestoreTestEnvironment.endpoint())
                .setHost(com.cover.time2gather.support.FirestoreTestEnvironment.endpoint()).build().getService();
        votes = new FirestoreMeetingUserSelectionRepository(db);
        meeting = new FirestoreMeetingRepository(db).save(Meeting.create(
                "mtg_" + UUID.randomUUID().toString().replace("-", ""), "Vote concurrency", "", 1L,
                "UTC", SelectionType.TIME, 60, Map.of(DATE, new int[]{9, 10, 12}), false));
    }

    @AfterEach void close() throws Exception { if (db != null) db.close(); }

    private MeetingUserSelection vote(long user, int slot) {
        return MeetingUserSelection.create(meeting.getId(), user, Map.of(DATE, new int[]{slot}));
    }

    private List<QueryDocumentSnapshot> documents() throws Exception {
        return db.collection("meetingUserSelections").whereEqualTo("meetingId", meeting.getId()).get().get().getDocuments();
    }

    private void concurrent(List<Callable<Void>> tasks) throws Exception {
        var ready = new CountDownLatch(tasks.size());
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(tasks.size())) {
            List<Future<Void>> pending = new ArrayList<>();
            for (var task : tasks) pending.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start barrier timed out");
                return task.call();
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            for (var task : pending) task.get(30, TimeUnit.SECONDS);
        }
    }

    @Test void simultaneousFirstSavesCreateOneRecordAndLaterEditsDoNotResurrectOldSlots() throws Exception {
        var tasks = new ArrayList<Callable<Void>>();
        for (int i = 0; i < 12; i++) tasks.add(() -> { votes.save(vote(2L, 9)); return null; });
        concurrent(tasks);
        assertThat(documents()).hasSize(1);
        votes.save(vote(2L, 12));
        assertThat(votes.findByMeetingIdAndUserId(meeting.getId(), 2L).orElseThrow().getSelections().get(DATE)).containsExactly(12);
        assertThat(votes.findAllByMeetingId(meeting.getId())).hasSize(1)
                .allSatisfy(value -> assertThat(value.getSelections().get(DATE)).containsExactly(12));
        assertThat(documents()).hasSize(1);
    }

    @Test void distinctParticipantsAndMeetingsRemainIndependent() throws Exception {
        var tasks = new ArrayList<Callable<Void>>();
        for (long user = 2; user <= 9; user++) {
            long id = user;
            tasks.add(() -> { votes.save(vote(id, 9)); return null; });
        }
        concurrent(tasks);
        votes.save(vote(2, 12));
        var other = new FirestoreMeetingRepository(db).save(Meeting.create(
                "mtg_" + UUID.randomUUID().toString().replace("-", ""), "Other", "", 1L,
                "UTC", SelectionType.TIME, 60, Map.of(DATE, new int[]{10}), false));
        votes.save(MeetingUserSelection.create(other.getId(), 2L, Map.of(DATE, new int[]{10})));
        assertThat(documents()).hasSize(8);
        assertThat(votes.findAllByUserId(2L)).hasSize(2);
        assertThat(votes.findAllByUserIdIn(List.of(2L, 3L))).hasSize(3);
        assertThat(votes.findAllByMeetingIdIn(List.of(meeting.getId(), other.getId()))).hasSize(9);
        assertThat(votes.findByMeetingIdAndUserId(meeting.getId(), 3L).orElseThrow().getSelections().get(DATE)).containsExactly(9);
    }

    private void legacy(long id, long user, String updated, boolean unavailable) throws Exception {
        Map<String, Object> fields = new HashMap<>(Map.of("id", id, "meetingId", meeting.getId(), "userId", user,
                "selectionType", "TIME", "intervalMinutes", 60,
                "createdAt", "2026-01-01T00:00:00", "updatedAt", updated));
        fields.put("selections", unavailable ? Map.of() : Map.of(DATE, List.of(9)));
        if (unavailable) fields.put("unavailableDates", List.of(DATE));
        db.collection("meetingUserSelections").document(Long.toString(id)).set(fields).get();
    }

    @Test void legacyDuplicatesUseLatestModificationInPersonalAggregateAndAdminReads() throws Exception {
        legacy(901, 2, "2026-11-01T00:00:00", false);
        legacy(101, 2, "2026-11-02T00:00:00", true); // latest ID sorts before the older record
        var latest = votes.findByMeetingIdAndUserId(meeting.getId(), 2L).orElseThrow();
        assertThat(latest.getId()).isEqualTo(101L);
        assertThat(latest.availabilityResponse(meeting)).isEqualTo("NONE");
        for (var found : List.of(votes.findAllByMeetingId(meeting.getId()), votes.findAllByUserId(2L),
                votes.findAllByUserIdIn(List.of(2L)), votes.findAllByMeetingIdIn(List.of(meeting.getId())))) {
            assertThat(found).hasSize(1).allSatisfy(value -> assertThat(value.getId()).isEqualTo(101L));
        }
        latest.updateSelections(Map.of(DATE, new int[]{12}));
        votes.save(latest);
        assertThat(documents()).hasSize(1);
        assertThat(votes.findByMeetingIdAndUserId(meeting.getId(), 2L).orElseThrow().getUnavailableDates()).isNull();
        assertThat(votes.findAllByMeetingId(meeting.getId()).getFirst().getSelections().get(DATE)).containsExactly(12);
    }

    @Test void noneAndTimeResponsesShareOneRecordAndDeletionRemovesLegacyCopies() throws Exception {
        legacy(901, 2, "2026-11-01T00:00:00", false);
        votes.save(MeetingUserSelection.unavailable(meeting, 2L, List.of(DATE)));
        assertThat(documents()).hasSize(1);
        assertThat(votes.findAllByMeetingId(meeting.getId()).getFirst().availabilityResponse(meeting)).isEqualTo("NONE");
        legacy(902, 2, "2026-11-01T00:00:00", false);
        votes.delete(votes.findByMeetingIdAndUserId(meeting.getId(), 2L).orElseThrow());
        assertThat(documents()).isEmpty();
        assertThat(votes.findByMeetingIdAndUserId(meeting.getId(), 2L)).isEmpty();
    }

    @Test void allDayConcurrentFirstSavesAndSubsequentDateChangesKeepOneRecord() throws Exception {
        var tasks = new ArrayList<Callable<Void>>();
        for (int i = 0; i < 8; i++) tasks.add(() -> {
            votes.save(MeetingUserSelection.create(meeting.getId(), 2L, SelectionType.ALL_DAY, 60, Map.of(DATE, new int[0])));
            return null;
        });
        concurrent(tasks);
        var saved = votes.findByMeetingIdAndUserId(meeting.getId(), 2L).orElseThrow();
        saved.updateSelections(Map.of("2027-01-11", new int[0])); votes.save(saved);
        assertThat(documents()).hasSize(1);
        assertThat(votes.findAllByMeetingId(meeting.getId()).getFirst().getSelections()).containsOnlyKeys("2027-01-11");
    }
}

package com.cover.time2gather.infra.identity;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.infra.firestore.FirestoreMeetingRepository;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.Tag("firestore")
class FirestoreMeetingNameStoreEmulatorTest {
    private Firestore db;
    private FirestoreMeetingNameStore names;
    private Meeting meeting;
    @BeforeEach void setup() {
        db = FirestoreOptions.newBuilder().setProjectId("demo-time2gather-names")
                .setEmulatorHost(com.cover.time2gather.support.FirestoreTestEnvironment.endpoint())
                .setHost(com.cover.time2gather.support.FirestoreTestEnvironment.endpoint()).build().getService();
        names = new FirestoreMeetingNameStore(db);
        meeting = new FirestoreMeetingRepository(db).save(Meeting.create("mtg_"+UUID.randomUUID().toString().replace("-",""),
                "Names", "", 1L, "Asia/Seoul", SelectionType.ALL_DAY, 60, Map.of("2027-01-10", new int[0]), false));
    }
    @AfterEach void close() throws Exception { db.close(); }
    @Test void namesSignalsAndLegacyFallbackAreIndependentFromVotes() throws Exception {
        assertThat(names.name(meeting.getId(),2L)).isEmpty();
        assertThat(names.names(meeting.getId())).isEmpty();assertThat(names.version(meeting.getId())).isZero();
        names.save(meeting.getId(),meeting.getMeetingCode(),2L,"Friends",false);
        assertThat(names.name(meeting.getId(),2L)).contains("Friends");
        long version=names.version(meeting.getId());assertThat(version).isPositive();
        assertThat(names.save(meeting.getId(),meeting.getMeetingCode(),2L,"Default",true)).isEqualTo("Friends");
        assertThat(names.version(meeting.getId())).isEqualTo(version);
        names.save(meeting.getId(),meeting.getMeetingCode(),2L,"Changed",false);
        names.save(meeting.getId(),meeting.getMeetingCode(),3L,"Changed",false);
        assertThat(names.names(meeting.getId())).containsExactlyInAnyOrderEntriesOf(Map.of(2L,"Changed",3L,"Changed"));
        assertThat(names.version(meeting.getId())).isGreaterThan(version);
        assertThat(db.collection("meetingUserSelections").whereEqualTo("meetingId",meeting.getId()).get().get().isEmpty()).isTrue();
        assertThatThrownBy(()->names.save(meeting.getId(),"mtg_wrong123",2L,"Bad",false)).isInstanceOf(IllegalStateException.class);
        assertThat(names.names(meeting.getId()).get(2L)).isEqualTo("Changed");
    }
    @Test void newMeetingAndHostNameAreCommittedTogether() throws Exception {
        var repository = new FirestoreMeetingRepository(db);
        var created = Meeting.create("mtg_"+UUID.randomUUID().toString().replace("-",""),
                "Atomic name", "", 1L, "Asia/Seoul", SelectionType.ALL_DAY, 60, Map.of("2027-01-10", new int[0]), false);
        repository.saveWithHostName(created,"Host alias");
        assertThat(repository.findById(created.getId())).isPresent();
        assertThat(names.name(created.getId(),1L)).contains("Host alias");
        var rejected = Meeting.create("mtg_"+UUID.randomUUID().toString().replace("-",""),
                "Rejected", "", 1L, "Asia/Seoul", SelectionType.ALL_DAY, 60, Map.of("2027-01-10", new int[0]), false);
        assertThatThrownBy(()->repository.saveWithHostName(rejected,null)).isInstanceOf(IllegalStateException.class);
        assertThat(repository.findById(rejected.getId())).isEmpty();
        assertThat(names.name(rejected.getId(),1L)).isEmpty();
    }
    @Test void concurrentDefaultCreationCannotOverwriteAnExplicitName() throws Exception {
        names.save(meeting.getId(),meeting.getMeetingCode(),2L,"Explicit",false);
        try (var executor=Executors.newFixedThreadPool(4)) {
            var calls=new ArrayList<Callable<String>>();
            for(int i=0;i<8;i++)calls.add(()->names.save(meeting.getId(),meeting.getMeetingCode(),2L,"Fallback",true));
            for(var future:executor.invokeAll(calls))assertThat(future.get()).isEqualTo("Explicit");
        }
        assertThat(names.names(meeting.getId())).containsExactlyEntriesOf(Map.of(2L,"Explicit"));
    }
}

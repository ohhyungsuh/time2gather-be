package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.meeting.*;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.Tag("firestore")
class UnavailableResponseFirestoreTest {
    @Test void explicitNoneAndLegacySelectionsRoundTripWithoutInventingAvailability() throws Exception {
        try(var db=FirestoreOptions.newBuilder().setProjectId("demo-time2gather-none")
                .setEmulatorHost(com.cover.time2gather.support.FirestoreTestEnvironment.endpoint()).setHost(com.cover.time2gather.support.FirestoreTestEnvironment.endpoint()).build().getService()) {
            var meetings=new FirestoreMeetingRepository(db);
            var meeting=meetings.save(Meeting.create("mtg_"+UUID.randomUUID().toString().replace("-",""),"None","",1L,"Asia/Seoul",SelectionType.TIME,60,Map.of("2027-01-10",new int[]{10}),false));
            var repo=new FirestoreMeetingUserSelectionRepository(db);
            var value=repo.save(MeetingUserSelection.unavailable(meeting,2L,List.of("2027-01-10")));
            var saved=repo.findByMeetingIdAndUserId(meeting.getId(),2L).orElseThrow();
            assertThat(saved.getSelections()).isEmpty();assertThat(saved.availabilityResponse(meeting)).isEqualTo("NONE");
            meeting.updateAvailableDates(List.of("2027-01-10","2027-01-11"));
            assertThat(saved.availabilityResponse(meeting)).isEqualTo("NEEDS_RESPONSE");
            saved.updateSelections(Map.of("2027-01-10",new int[]{10}));repo.save(saved);
            assertThat(repo.findByMeetingIdAndUserId(meeting.getId(),2L).orElseThrow().getUnavailableDates()).isNull();
            db.collection("meetingUserSelections").document(FirestoreMeetingUserSelectionRepository.documentId(meeting.getId(), 2L)).update("unavailableDates",FieldValue.delete()).get();
            assertThat(repo.findByMeetingIdAndUserId(meeting.getId(),2L).orElseThrow().getUnavailableDates()).isNull();
            repo.delete(saved);
            db.collection("meetings").document(meeting.getId().toString()).delete().get();
        }
    }
}

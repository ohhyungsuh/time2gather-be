package com.cover.time2gather.integration;

import com.cover.time2gather.support.FirestoreIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class MeetingCandidatesFirestoreIntegrationTest extends FirestoreIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired com.google.cloud.firestore.Firestore db;
    @Autowired com.cover.time2gather.infra.meeting.MeetingRepository meetings;
    @Autowired com.cover.time2gather.infra.meeting.MeetingCandidateStore candidateStore;
    @Autowired com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository selections;
    record Session(Cookie cookie, String id) { }
    JsonNode data(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsByteArray()).get("data");
    }
    Session login(String name) throws Exception {
        var result=mvc.perform(post("/api/v1/auth/anonymous").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username",name))))
                .andExpect(status().isOk()).andReturn();
        return new Session(result.getResponse().getCookie("__session"), data(result).get("userId").asText());
    }
    String create(Session host, String type, boolean locationVoting) throws Exception {
        var body=new HashMap<String,Object>();
        body.put("title","Candidate management integration");body.put("timezone","Asia/Seoul");
        body.put("selectionType",type);body.put("intervalMinutes",60);
        body.put("availableDates",Map.of("2027-01-04",type.equals("TIME")?List.of("09:00","10:00"):List.of(),
                "2027-01-05",type.equals("TIME")?List.of("09:00","10:00"):List.of()));
        body.put("locationVoteEnabled",locationVoting);
        if(locationVoting)body.put("locations",List.of("A","B","C"));
        return data(mvc.perform(post("/api/v1/meetings").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk()).andReturn()).get("meetingCode").asText();
    }
    String path(String code){return "/api/v1/meetings/"+code;}
    JsonNode detail(String code) throws Exception {return data(mvc.perform(get(path(code))).andExpect(status().isOk()).andReturn());}
    Map<String,Object> command(JsonNode before,List<String> dates,List<String> times,List<Map<String,Object>> locations){
        var body=new HashMap<String,Object>();body.put("dates",dates);
        body.put("expectedVersion",before.path("candidateVersion").asText("0".repeat(64)));
        if(times!=null)body.put("times",times);
        if(locations!=null)body.put("locations",locations);
        return body;
    }
    List<Map<String,Object>> samePlaces(JsonNode before){
        var result=new ArrayList<Map<String,Object>>();
        for(var place:before.at("/locationVote/locations"))result.add(Map.of("id",place.get("id").asText(),"name",place.get("name").asText()));
        return result;
    }
    void vote(String code,Session user,List<String> times,List<String> ids) throws Exception {
        mvc.perform(put(path(code)+"/selections").cookie(user.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("selections",List.of(Map.of("date","2027-01-04","type","TIME","times",times))))))
                .andExpect(status().isOk());
        mvc.perform(put(path(code)+"/location-selections").cookie(user.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("locationIds",ids)))).andExpect(status().isOk());
    }
    @Test void updatesDatesTimesAndPlacesTogetherAndPreservesOnlyRetainedVotes() throws Exception {
        var host=login("Host");var guest=login("Guest");var code=create(host,"TIME",true);var before=detail(code);
        var oldPlaces=samePlaces(before);String a=(String)oldPlaces.get(0).get("id"), b=(String)oldPlaces.get(1).get("id");
        vote(code,guest,List.of("09:00","10:00"),List.of(a,b));
        var next=List.of(oldPlaces.get(0),Map.<String,Object>of("id",b,"name","B renamed"),Map.<String,Object>of("name","D"));
        mvc.perform(put(path(code)+"/candidates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(command(before,List.of("2027-01-04","2027-01-06"),List.of("10:00","11:00"),next))))
                .andExpect(status().isOk());
        var after=detail(code);
        assertThat(after.at("/meeting/availableDates").has("2027-01-05")).isFalse();
        assertThat(after.at("/meeting/availableDates/2027-01-06/1").asText()).isEqualTo("11:00");
        assertThat(after.at("/schedule/2027-01-04").has("09:00")).isFalse();
        assertThat(after.at("/schedule/2027-01-04/10:00/count").asInt()).isEqualTo(1);
        assertThat(after.at("/locationVote/locations/0/id").asText()).isEqualTo(a);
        assertThat(after.at("/locationVote/locations/0/voteCount").asInt()).isEqualTo(1);
        assertThat(after.at("/locationVote/locations/1/name").asText()).isEqualTo("B renamed");
        assertThat(after.at("/locationVote/locations/1/id").asText()).isNotEqualTo(b);
        assertThat(after.at("/locationVote/locations/1/voteCount").asInt()).isZero();
        var personal=data(mvc.perform(get(path(code)+"/location-selections").cookie(guest.cookie())).andExpect(status().isOk()).andReturn());
        assertThat(personal.get("locationIds").size()).isEqualTo(1);
        assertThat(personal.at("/locationIds/0").asText()).isEqualTo(a);
    }
    @Test void rejectedRequestsPreserveEveryCandidateAndBallot() throws Exception {
        var host=login("Host");var guest=login("Guest");var code=create(host,"TIME",true);var before=detail(code);
        var places=samePlaces(before);vote(code,guest,List.of("09:00"),List.of((String)places.getFirst().get("id")));
        before=detail(code);
        var invalid=new ArrayList<Map<String,Object>>(places);invalid.set(1,Map.of("id","9223372036854775807","name","Foreign"));
        var body=command(before,List.of("2027-01-04"),List.of("10:00"),invalid);
        mvc.perform(put(path(code)+"/candidates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().is4xxClientError());
        assertThat(detail(code)).isEqualTo(before);
        body=command(before,List.of("2027-01-04"),List.of("10:00"),places);
        mvc.perform(put(path(code)+"/candidates").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isForbidden());
        assertThat(detail(code)).isEqualTo(before);
        for(var replacement:List.of(List.of(places.getFirst()),List.of(places.getFirst(),places.getFirst()))) {
            mvc.perform(put(path(code)+"/candidates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(command(before,List.of("2027-01-04"),List.of("10:00"),replacement)))).andExpect(status().is4xxClientError());
            assertThat(detail(code)).isEqualTo(before);
        }
    }
    @Test void staleEditorCannotOverwriteNewerCandidatesOrReuseRenamedPlace() throws Exception {
        var host=login("Host");var guest=login("Guest");var code=create(host,"TIME",true);var before=detail(code);var places=samePlaces(before);
        var edited=new ArrayList<Map<String,Object>>(places);edited.set(0,Map.of("id",places.getFirst().get("id"),"name","New meaning"));
        var body=command(before,List.of("2027-01-04"),List.of("10:00"),edited);
        mvc.perform(put(path(code)+"/candidates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isOk());
        var after=detail(code);
        mvc.perform(put(path(code)+"/candidates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isConflict());
        mvc.perform(put(path(code)+"/location-selections").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("locationIds",List.of(places.getFirst().get("id")))))).andExpect(status().is4xxClientError());
        assertThat(detail(code)).isEqualTo(after);
    }
    @Test void dateOnlyMeetingNeedsNoPlacePayloadAndRetainsUnchangedVotes() throws Exception {
        var host=login("Host");var guest=login("Guest");var code=create(host,"ALL_DAY",false);var before=detail(code);
        mvc.perform(put(path(code)+"/selections").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("selections",List.of(Map.of("date","2027-01-04","type","ALL_DAY","times",List.of())))))).andExpect(status().isOk());
        mvc.perform(put(path(code)+"/candidates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(command(before,List.of("2027-01-04","2027-01-06"),null,null))))
                .andExpect(status().isOk());
        assertThat(detail(code).at("/schedule/2027-01-04/ALL_DAY/count").asInt()).isEqualTo(1);
    }
    @Test void concurrentEditorsAcceptOneCompleteCandidateSet() throws Exception {
        var host=login("Host");var code=create(host,"TIME",true);var before=detail(code);
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var gate=new java.util.concurrent.CountDownLatch(1);var tasks=new ArrayList<java.util.concurrent.Future<Integer>>();
            for(int i=0;i<2;i++) {final int index=i;tasks.add(executor.submit(()->{
                gate.await();return mvc.perform(put(path(code)+"/candidates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(command(before,List.of("2027-01-0"+(index+6)),List.of(index==0?"11:00":"12:00"),
                                List.of(Map.of("name","Set "+index+" A"),Map.of("name","Set "+index+" B")))))).andReturn().getResponse().getStatus();}));}
            gate.countDown();assertThat(List.of(tasks.get(0).get(),tasks.get(1).get())).containsExactlyInAnyOrder(200,409);
        }
        var after=detail(code);String name=after.at("/locationVote/locations/0/name").asText();
        assertThat(after.at("/meeting/availableDates/"+(name.contains("0")?"2027-01-06":"2027-01-07")+"/0").asText()).isEqualTo(name.contains("0")?"11:00":"12:00");
    }

    @Test @SuppressWarnings({"unchecked","rawtypes"})
    void failedAtomicCommitLeavesDatesPlacesVotesAndVersionUntouched() throws Exception {
        var host=login("Host");var guest=login("Guest");var code=create(host,"TIME",true);var before=detail(code);var places=samePlaces(before);
        vote(code,guest,List.of("09:00"),List.of((String)places.getFirst().get("id")));before=detail(code);
        var failing=org.mockito.Mockito.mock(com.google.cloud.firestore.Firestore.class,org.mockito.AdditionalAnswers.delegatesTo(db));
        org.mockito.Mockito.doAnswer(invocation -> db.runTransaction(tx -> {
            Object result=((com.google.cloud.firestore.Transaction.Function)invocation.getArgument(0)).updateCallback(tx);
            tx.update(db.collection("missingCandidateCommit").document("absent"),"value",1);return result;
        })).when(failing).runTransaction(org.mockito.ArgumentMatchers.any(com.google.cloud.firestore.Transaction.Function.class));
        var store=new com.cover.time2gather.infra.firestore.FirestoreMeetingCandidateStore(failing);
        String version=before.get("candidateVersion").asText();
        assertThatThrownBy(()->store.update(code,Long.valueOf(host.id()),new com.cover.time2gather.domain.meeting.MeetingCandidates.Edit(
                List.of("2027-01-06"),List.of("12:00"),List.of(new com.cover.time2gather.domain.meeting.MeetingCandidates.Location(null,"New A"),
                new com.cover.time2gather.domain.meeting.MeetingCandidates.Location(null,"New B")),version))).isInstanceOf(IllegalStateException.class);
        assertThat(detail(code)).isEqualTo(before);
    }
    @Test void timeVoteValidatedBeforeCandidateEditCannotRestoreRemovedSlots() throws Exception {
        var host=login("Host");var guest=login("Guest");var code=create(host,"TIME",true);var before=detail(code);
        var oldMeeting=meetings.findByMeetingCode(code).orElseThrow();
        mvc.perform(put(path(code)+"/candidates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(command(before,List.of("2027-01-04"),List.of("10:00"),samePlaces(before))))).andExpect(status().isOk());
        var after=detail(code);
        var oldBallot=com.cover.time2gather.domain.meeting.MeetingUserSelection.create(oldMeeting.getId(),Long.valueOf(guest.id()),Map.of("2027-01-04",new int[]{9}));
        assertThatThrownBy(()->selections.saveForCandidates(oldBallot,oldMeeting)).isInstanceOf(com.cover.time2gather.domain.exception.BusinessException.class);
        assertThat(detail(code)).isEqualTo(after);
    }

}

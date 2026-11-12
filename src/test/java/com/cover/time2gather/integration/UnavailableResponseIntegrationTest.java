package com.cover.time2gather.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class UnavailableResponseIntegrationTest extends com.cover.time2gather.support.FirestoreIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    record Session(Cookie cookie, String id) { }
    JsonNode data(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsByteArray()).get("data"); }
    Session login(String name) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/anonymous").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", name)))).andExpect(status().isOk()).andReturn();
        return new Session(result.getResponse().getCookie("__session"), data(result).get("userId").asText());
    }
    String create(Session host, String type) throws Exception {
        return data(mvc.perform(post("/api/v1/meetings").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title","Unavailable QA","selectionType",type,"timezone","Asia/Seoul","intervalMinutes",60,
                "availableDates",Map.of("2027-01-10",type.equals("TIME")?List.of("10:00","11:00"):List.of()),
                "locationVoteEnabled",true,"locations",List.of("Cafe","Park")))))
                .andExpect(status().isOk()).andReturn()).get("meetingCode").asText();
    }
    String path(String code) { return "/api/v1/meetings/"+code; }
    JsonNode detail(String code) throws Exception { return data(mvc.perform(get(path(code))).andExpect(status().isOk()).andReturn()); }
    void none(String code, Session guest, List<String> dates) throws Exception {
        mvc.perform(put(path(code)+"/unavailable").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("candidateDates",dates)))).andExpect(status().isOk());
    }
    void vote(String code,Session guest,String type) throws Exception {
        mvc.perform(put(path(code)+"/selections").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("selections",List.of(Map.of("date","2027-01-10","type",type,"times",type.equals("TIME")?List.of("10:00"):List.of()))))))
            .andExpect(status().isOk());
    }
    String response(JsonNode d, Session session) {
        for(JsonNode p:d.get("participants")) if(p.get("userId").asText().equals(session.id())) return p.get("availabilityResponse").isNull()?null:p.get("availabilityResponse").asText();
        return null;
    }
    @Test void allDayAndTimeRoundTripCountsAndLocationPreservation() throws Exception {
        for(String type:List.of("ALL_DAY","TIME")) {
            var host=login("Host");var guest=login("Guest");var code=create(host,type);
            var locationId=detail(code).at("/locationVote/locations/0/id").asText();
            mvc.perform(put(path(code)+"/location-selections").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"locationIds\":[\""+locationId+"\"]}")).andExpect(status().isOk());
            vote(code,host,type);vote(code,guest,type);none(code,guest,List.of("2027-01-10"));none(code,guest,List.of("2027-01-10"));
            var d=detail(code);assertThat(d.at("/summary/totalParticipants").asInt()).isEqualTo(2);
            assertThat(d.at("/summary/bestSlots/0/count").asInt()).isEqualTo(1);
            assertThat(d.at("/summary/bestSlots/0/percentage").asText()).isEqualTo("50%");
            assertThat(response(d,guest)).isEqualTo("NONE");
            assertThat(d.at("/locationVote/locations/0/voteCount").asInt()).isEqualTo(1);
            mvc.perform(get(path(code)).cookie(guest.cookie())).andExpect(jsonPath("$.data.participated").value(true));
            vote(code,guest,type);assertThat(response(detail(code),guest) == null).isTrue();
            assertThat(detail(code).at("/summary/bestSlots/0/count").asInt()).isEqualTo(2);
        }
    }
    @Test void newCandidatesRequireResponseAndStaleSubmissionIsRejected() throws Exception {
        var host=login("Host");var guest=login("Guest");var code=create(host,"ALL_DAY");none(code,guest,List.of("2027-01-10"));
        mvc.perform(put(path(code)+"/available-dates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"dates\":[\"2027-01-10\",\"2027-01-11\"]}")).andExpect(status().isOk());
        assertThat(response(detail(code),guest)).isEqualTo("NEEDS_RESPONSE");
        mvc.perform(put(path(code)+"/unavailable").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"candidateDates\":[\"2027-01-10\"]}")).andExpect(status().isConflict());
        none(code,guest,List.of("2027-01-10","2027-01-11"));
        assertThat(response(detail(code),guest)).isEqualTo("NONE");
        assertThat(detail(code).at("/summary/bestSlots").size()).isZero();
    }
    @Test void editTimesRoundTripPreservesVotesAndInvalidatesUnavailableResponse() throws Exception {
        var host=login("Host"); var guest=login("Guest"); var code=create(host,"TIME");
        vote(code,host,"TIME"); none(code,guest,List.of("2027-01-10"));
        mvc.perform(put(path(code)+"/available-dates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"dates\":[\"2027-01-10\",\"2027-01-11\"],\"times\":[\"10:00\",\"11:00\",\"12:00\"]}"))
            .andExpect(status().isOk());
        var d=detail(code);
        assertThat(d.at("/meeting/availableDates/2027-01-11/2").asText()).isEqualTo("12:00");
        assertThat(d.at("/summary/bestSlots/0/time").asText()).isEqualTo("10:00");
        assertThat(response(d,guest)).isEqualTo("NEEDS_RESPONSE");
        mvc.perform(put(path(code)+"/available-dates").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"dates\":[\"2027-01-10\"],\"times\":[\"11:00\"]}"))
            .andExpect(status().isForbidden());
        for (String times : List.of("[]", "[\"24:00\"]", "[\"09:30\"]", "[null]")) {
            mvc.perform(put(path(code)+"/available-dates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dates\":[\"2027-01-10\"],\"times\":"+times+"}"))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(put(path(code)+"/available-dates").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"dates\":[\"2027-01-10\"],\"times\":[\"11:00\"]}"))
            .andExpect(status().isOk());
        assertThat(detail(code).at("/summary/bestSlots").size()).isZero();
    }

    @Test void authenticationValidationAndConfirmationRemainEnforced() throws Exception {
        var host=login("Host");var guest=login("Guest");var code=create(host,"ALL_DAY");
        mvc.perform(put(path(code)+"/unavailable").contentType(MediaType.APPLICATION_JSON).content("{\"candidateDates\":[\"2027-01-10\"]}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put(path(code)+"/unavailable").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON).content("{\"candidateDates\":[]}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put(path(code)+"/selections").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON).content("{\"selections\":[]}"))
            .andExpect(status().isBadRequest());
        vote(code,host,"ALL_DAY");var locationId=detail(code).at("/locationVote/locations/0/id").asText();
        mvc.perform(put(path(code)+"/confirm").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"date\":\"2027-01-10\",\"slotIndex\":-1,\"locationId\":\""+locationId+"\"}"))
            .andExpect(status().isOk());
        mvc.perform(put(path(code)+"/unavailable").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON).content("{\"candidateDates\":[\"2027-01-10\"]}"))
            .andExpect(status().is4xxClientError());
    }
}

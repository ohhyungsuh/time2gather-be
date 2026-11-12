package com.cover.time2gather.integration;

import com.cover.time2gather.domain.user.UserRepository;
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
class MeetingNamesIntegrationTest extends com.cover.time2gather.support.FirestoreIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    record Session(Cookie cookie, long id) { }
    JsonNode data(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsByteArray()).get("data"); }
    Session login(String name) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/anonymous").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", name)))).andExpect(status().isOk()).andReturn();
        return new Session(result.getResponse().getCookie("__session"), data(result).get("userId").asLong());
    }
    String meeting(Session host, String hostName) throws Exception {
        var request = new HashMap<String,Object>(Map.of("title", "Names", "selectionType", "ALL_DAY", "availableDates", Map.of("2026-12-01", List.of()),
                "locationVoteEnabled", true, "locations", List.of("Cafe", "Park")));
        if (hostName != null) request.put("hostName", hostName);
        return data(mvc.perform(post("/api/v1/meetings").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andExpect(status().isOk()).andReturn()).get("meetingCode").asText();
    }
    String path(String code) { return "/api/v1/meetings/" + code; }
    JsonNode detail(String code) throws Exception { return data(mvc.perform(get(path(code))).andExpect(status().isOk()).andReturn()); }
    void rename(String code, Session session, String name) throws Exception {
        mvc.perform(put(path(code)+"/profile").cookie(session.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", name, "userId", -123))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value(name.trim()))
                .andExpect(header().string("Cache-Control", "private, no-store"));
    }
    void vote(String code, Session session) throws Exception {
        mvc.perform(put(path(code)+"/selections").cookie(session.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"selections\":[{\"date\":\"2026-12-01\",\"type\":\"ALL_DAY\",\"times\":[]}]}"))
                .andExpect(status().isOk());
    }
    JsonNode comment(String code, Session session, String body, List<?> mentions) throws Exception {
        return data(mvc.perform(post(path(code)+"/comments").cookie(session.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("body",body,"mentions",mentions,"requestId",UUID.randomUUID().toString()))))
                .andExpect(status().isOk()).andReturn());
    }
    @Test void legacyFallbackScopedNamesAndStableOwnership() throws Exception {
        var host=login("Default host"); var guest=login("Default guest");
        var a=meeting(host, null); var b=meeting(host, "Host B");
        mvc.perform(get(path(a)+"/profile").cookie(guest.cookie())).andExpect(jsonPath("$.data.username").value("Default guest"));
        assertThat(detail(a).at("/meeting/host/username").asText()).isEqualTo("Default host");
        assertThat(detail(b).at("/meeting/host/username").asText()).isEqualTo("Host B");
        rename(a,guest,"  Team name  ");
        assertThat(detail(a).at("/summary/totalParticipants").asInt()).isZero();
        JsonNode comment=comment(a,guest,"Hello",List.of());
        assertThat(comment.at("/author/name").asText()).isEqualTo("Team name");
        assertThat(detail(a).at("/summary/totalParticipants").asInt()).isZero();
        vote(a,guest); vote(a,guest); vote(b,guest);
        rename(a,guest,"Friends name");
        mvc.perform(put(path(a)+"/profile").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"Stale name from another tab\",\"onlyIfAbsent\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("Friends name"));
        var detail=detail(a);
        assertThat(detail.at("/summary/totalParticipants").asInt()).isEqualTo(1);
        assertThat(detail.at("/participants/0/userId").asLong()).isEqualTo(guest.id());
        assertThat(detail.at("/participants/0/username").asText()).isEqualTo("Friends name");
        assertThat(detail.at("/schedule/2026-12-01/ALL_DAY/participants/0/username").asText()).isEqualTo("Friends name");
        assertThat(detail.at("/summary/bestSlots/0/participants/0/username").asText()).isEqualTo("Friends name");
        assertThat(detail(b).at("/participants/0/username").asText()).isEqualTo("Default guest");
        mvc.perform(get(path(a)+"/comments/"+comment.get("id").asText()).cookie(guest.cookie()))
                .andExpect(jsonPath("$.data.author.name").value("Friends name")).andExpect(jsonPath("$.data.canEdit").value(true));
        mvc.perform(put(path(a)+"/comments/"+comment.get("id").asText()).cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Edited\",\"mentions\":[]}"))
                .andExpect(status().isOk());
        // Presentation reads must not dirty-check and rename the managed global User entity.
        assertThat(users.findById(guest.id()).orElseThrow().getUsername()).isEqualTo("Default guest");
        assertThat(users.findById(host.id()).orElseThrow().getUsername()).isEqualTo("Default host");
    }
    @Test void duplicateNamesNeverMergePeopleAndMentionsKeepTheirOriginalText() throws Exception {
        var host=login("Host");var a=login("A");var b=login("B");var code=meeting(host,null);
        rename(code,a,"Same");rename(code,b,"Same");vote(code,a);vote(code,b);
        assertThat(detail(code).at("/summary/totalParticipants").asInt()).isEqualTo(2);
        var comment=comment(code,host,"@Same hello",List.of(Map.of("userId",Long.toString(a.id()),"start",0,"end",5)));
        var before=data(mvc.perform(get(path(code)+"/comments/summary")).andReturn());
        rename(code,a,"Renamed");
        var after=data(mvc.perform(get(path(code)+"/comments/summary")).andReturn());
        assertThat(after.get("version")).isEqualTo(before.get("version"));
        assertThat(after.get("namesVersion").asLong()).isGreaterThan(before.get("namesVersion").asLong());
        mvc.perform(get(path(code)+"/comments/"+comment.get("id").asText()))
                .andExpect(jsonPath("$.data.body").value("@Same hello"))
                .andExpect(jsonPath("$.data.mentions[0].userId").value(Long.toString(a.id())));
        mvc.perform(get(path(code)+"/comments/people"))
                .andExpect(jsonPath("$.data[?(@.id == '"+a.id()+"')].name").value("Renamed"));
        mvc.perform(put(path(code)+"/comments/"+comment.get("id").asText()).cookie(a.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Not mine\",\"mentions\":[]}"))
                .andExpect(status().isForbidden());
    }
    @Test void locationOnlyNamesDoNotAddScheduleParticipants() throws Exception {
        var host=login("Host");var guest=login("Guest");var code=meeting(host,null);
        rename(code,guest,"Location voter");
        var location=detail(code).at("/locationVote/locations/0/id").asLong();
        mvc.perform(put(path(code)+"/location-selections").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("locationIds",List.of(location)))))
                .andExpect(status().isOk());
        var d=detail(code);
        assertThat(d.at("/summary/totalParticipants").asInt()).isZero();
        assertThat(d.at("/locationVote/locations/0/voters/0/username").asText()).isEqualTo("Location voter");
    }
    @Test void legacyGlobalRenameCannotOverwriteScopedNamesOrCommentIdentity() throws Exception {
        var host=login("Host");var guest=login("Original");var a=meeting(host,null);var b=meeting(host,null);
        rename(a,guest,"Scoped");
        comment(b,guest,"Comment-only",List.of());
        mvc.perform(put("/api/v1/auth/me").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"Legacy new default\"}")).andExpect(status().isOk());
        mvc.perform(get(path(a)+"/profile").cookie(guest.cookie())).andExpect(jsonPath("$.data.username").value("Scoped"));
        mvc.perform(get(path(b)+"/profile").cookie(guest.cookie())).andExpect(jsonPath("$.data.username").value("Original"));
        vote(a,guest);
        var location=detail(a).at("/locationVote/locations/0/id").asLong();
        mvc.perform(put(path(a)+"/confirm").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("date","2026-12-01","locationId",location))))
                .andExpect(status().isOk());
        rename(a,guest,"After confirmation");
        assertThat(detail(a).at("/participants/0/username").asText()).isEqualTo("After confirmation");
        assertThat(detail(a).at("/meeting/confirmedDate").asText()).isEqualTo("2026-12-01");
        mvc.perform(put(path(a)+"/selections").cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"selections\":[]}")).andExpect(status().isBadRequest());
    }
    @Test void validationAndAuthenticationAreBackwardCompatible() throws Exception {
        var host=login("Host");var code=meeting(host,null);
        mvc.perform(get(path(code)+"/profile")).andExpect(status().isUnauthorized());
        for(String name:List.of("", "   ", "x".repeat(101))) {
            mvc.perform(put(path(code)+"/profile").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("username",name)))).andExpect(status().isBadRequest());
        }
        mvc.perform(put(path("missing")+"/profile").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"Name\"}")).andExpect(status().isNotFound());
    }
}

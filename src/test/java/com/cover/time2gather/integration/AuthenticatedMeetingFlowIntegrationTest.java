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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc


class AuthenticatedMeetingFlowIntegrationTest extends com.cover.time2gather.support.FirestoreIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void anonymousSessionSupportsTheCompleteMeetingFlow() throws Exception {
        Cookie sessionCookie = login("회귀 테스트 사용자");

        mockMvc.perform(get("/api/v1/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("회귀 테스트 사용자"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/meetings")
                .header("Accept-Language", "ja")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "인증 회귀 테스트",
                                  "timezone": "Asia/Seoul",
                                  "selectionType": "TIME",
                                  "intervalMinutes": 60,
                                  "availableDates": {"2026-10-20": ["09:00", "10:00"]},
                                  "locationVoteEnabled": true,
                                  "locations": ["회의실 A", "회의실 B"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode createData = responseData(createResult);
        String meetingCode = createData.get("meetingCode").asText();

        MvcResult detailResult = mockMvc.perform(get("/api/v1/meetings/{code}", meetingCode).header("Accept-Language", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meeting.creatorLocale").value("ja"))
                .andExpect(jsonPath("$.data.meeting.code").value(meetingCode))
                .andExpect(jsonPath("$.data.locationVote.locations.length()").value(2))
                .andReturn();

        long locationId = responseData(detailResult)
                .get("locationVote").get("locations").get(0).get("id").asLong();

        mockMvc.perform(put("/api/v1/meetings/{code}/selections", meetingCode)
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selections":[{"date":"2026-10-20","type":"TIME","times":["09:00"]}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/meetings/{code}/selections", meetingCode)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selections['2026-10-20'][0]").value("09:00"));

        mockMvc.perform(put("/api/v1/meetings/{code}/location-selections", meetingCode)
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationIds\":[" + locationId + "]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/meetings/{code}/location-selections", meetingCode)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationIds[0]").value(locationId));

        mockMvc.perform(put("/api/v1/meetings/{code}/confirm", meetingCode)
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-10-20","slotIndex":9,"locationId":%d}
                                """.formatted(locationId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/meetings/{code}", meetingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meeting.confirmedDate").value("2026-10-20"))
                .andExpect(jsonPath("$.data.meeting.confirmedTime").value("09:00"))
                .andExpect(jsonPath("$.data.locationVote.confirmedLocation.id").value(locationId));

        mockMvc.perform(put("/api/v1/meetings/{code}/confirmed-time", meetingCode)
                        .cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"time\":\"09:15\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/meetings/{code}", meetingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meeting.confirmedTime").value("09:15"))
                .andExpect(jsonPath("$.data.meeting.confirmedSlotIndex").value(9));

        mockMvc.perform(get("/api/v1/meetings/{code}/export", meetingCode)
                        .queryParam("date", "2026-10-20")
                        .queryParam("slotIndex", "9"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar"));
    }

    @Test
    void halfHourCandidatesPersistVotesAndExportThirtyMinutes() throws Exception {
        Cookie host = login("Half hour host");
        MvcResult created = mockMvc.perform(post("/api/v1/meetings").cookie(host)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Half hour poll","timezone":"Asia/Seoul","selectionType":"TIME",
                         "intervalMinutes":30,"availableDates":{"2026-11-15":["09:00","09:30","10:00","10:30"]}}
                        """))
                .andExpect(status().isOk()).andReturn();
        String code = responseData(created).get("meetingCode").asText();
        mockMvc.perform(put("/api/v1/meetings/{code}/selections", code).cookie(host)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"selections":[{"date":"2026-11-15","type":"TIME","times":["09:30"]}]}
                        """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/meetings/{code}/selections", code).cookie(host))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selections['2026-11-15'].length()").value(1))
                .andExpect(jsonPath("$.data.selections['2026-11-15'][0]").value("09:30"));
        mockMvc.perform(get("/api/v1/meetings/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meeting.intervalMinutes").value(30))
                .andExpect(jsonPath("$.data.meeting.availableDates['2026-11-15'].length()").value(4))
                .andExpect(jsonPath("$.data.schedule['2026-11-15']['09:30'].count").value(1));
        // A time outside the registered half-hours must not erase the saved vote.
        mockMvc.perform(put("/api/v1/meetings/{code}/selections", code).cookie(host)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"selections":[{"date":"2026-11-15","type":"TIME","times":["09:15"]}]}
                        """))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/v1/meetings/{code}/selections", code).cookie(host))
                .andExpect(jsonPath("$.data.selections['2026-11-15'][0]").value("09:30"));
        mockMvc.perform(put("/api/v1/meetings/{code}/confirm", code).cookie(host)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-11-15","slotIndex":19}
                        """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/meetings/{code}", code))
                .andExpect(jsonPath("$.data.meeting.confirmedTime").value("09:30"))
                .andExpect(jsonPath("$.data.meeting.confirmedSlotIndex").value(19));
        String calendar = mockMvc.perform(get("/api/v1/meetings/{code}/export", code)
                        .queryParam("date", "2026-11-15").queryParam("slotIndex", "19"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andReturn().getResponse().getContentAsString();
        assertThat(calendar).contains("DTSTART:20261115T003000Z", "DTEND:20261115T010000Z");
    }

    @Test
    void legacyAccessTokenCookieStillAuthenticatesExistingClients() throws Exception {
        Cookie issuedCookie = login("레거시 쿠키 사용자");
        Cookie legacyCookie = new Cookie("accessToken", issuedCookie.getValue());

        mockMvc.perform(get("/api/v1/auth/me").cookie(legacyCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("레거시 쿠키 사용자"));
    }

    @Test
    void legacyMeetingScopedLoginPathAndPasswordFieldRemainAccepted() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/meetings/{code}/auth/anonymous", "legacy-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"레거시 경로 사용자\",\"password\":\"old-client-field\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("레거시 경로 사용자"))
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("__session");
        assertThat(sessionCookie).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk());
    }

    @Test
    void protectedMeetingWritesRejectRequestsWithoutEitherSessionCookie() throws Exception {
        mockMvc.perform(post("/api/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private Cookie login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/anonymous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("username", username))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("__session");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
    }
}

package com.cover.time2gather.api.admin;

import com.cover.time2gather.domain.admin.AdminDashboardService;
import com.cover.time2gather.domain.admin.AdminSummary;
import com.cover.time2gather.domain.admin.AdminUsageService;
import com.cover.time2gather.domain.admin.AdminUsageStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import java.time.Instant;
import jakarta.servlet.http.Cookie;
import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.user.UserRole;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"admin.google.client-id=admin-test-client", "admin.allowed-emails=owner@gmail.com"})
@AutoConfigureMockMvc

class AdminControllerSecurityTest extends com.cover.time2gather.support.FirestoreIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private AdminUsageService adminUsageService;

    @MockitoBean
    private JwtDecoder adminGoogleJwtDecoder;

    @Autowired
    private JwtTokenService tokenService;

    @Test
    void shouldRejectUnauthenticatedAdminRequests() throws Exception {
        mockMvc.perform(get("/api/v1/admin/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(get("/api/v1/admin/usage")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectNormalUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnSummaryToAdmins() throws Exception {
        when(adminGoogleJwtDecoder.decode("google-token")).thenReturn(identity("owner@gmail.com"));
        when(adminDashboardService.getSummary()).thenReturn(new AdminSummary(
                new AdminSummary.UserStats(10L, 8L, 2L, 1L),
                new AdminSummary.MeetingStats(12L, 9L, 3L)
        ));

        mockMvc.perform(get("/api/v1/admin/summary").header("Authorization", "Bearer google-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users.total").value(10))
                .andExpect(jsonPath("$.data.meetings.active").value(9));
    }

    @Test
    void shouldReturnUsageOnlyToAllowedAdminsWithoutCaching() throws Exception {
        when(adminGoogleJwtDecoder.decode("google-token")).thenReturn(identity("owner@gmail.com"));
        when(adminGoogleJwtDecoder.decode("outsider")).thenReturn(identity("outsider@gmail.com"));
        when(adminUsageService.getUsage(java.util.Set.of(99L))).thenReturn(new AdminUsageStats(
                Instant.parse("2026-11-06T00:00:00Z"), "CURRENT_STORED_STATE_ALL_TIME", 2,
                4, 4, 3, 2, 1, 5, 3, 1, 0.5, 0.25, 1.0 / 3));
        mockMvc.perform(get("/api/v1/admin/usage").param("excludeHostUserIds", "99")
                        .header("Authorization", "Bearer google-token"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.activatedMeetings").value(2))
                .andExpect(jsonPath("$.data.activationRate").value(0.5))
                .andExpect(jsonPath("$.data.basis").value("CURRENT_STORED_STATE_ALL_TIME"));
        mockMvc.perform(get("/api/v1/admin/usage").header("Authorization", "Bearer outsider"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meetingEndpointPassesConfirmedFilterAndDefaultsToAll() throws Exception {
        when(adminGoogleJwtDecoder.decode("google-token")).thenReturn(identity("owner@gmail.com"));
        var empty = new com.cover.time2gather.domain.admin.AdminPage<com.cover.time2gather.domain.admin.AdminMeetingItem>(
                java.util.List.of(), 0, 20, 0, 0);
        when(adminDashboardService.searchMeetings("Dinner", 0, 20, null, true)).thenReturn(empty);
        when(adminDashboardService.searchMeetings("", 0, 20, null, false)).thenReturn(empty);
        mockMvc.perform(get("/api/v1/admin/meetings").param("query", "Dinner").param("confirmedOnly", "true")
                        .header("Authorization", "Bearer google-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/api/v1/admin/meetings").header("Authorization", "Bearer google-token"))
                .andExpect(status().isOk());
        org.mockito.Mockito.verify(adminDashboardService).searchMeetings("Dinner", 0, 20, null, true);
        org.mockito.Mockito.verify(adminDashboardService).searchMeetings("", 0, 20, null, false);
        mockMvc.perform(get("/api/v1/admin/meetings").param("confirmedOnly", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLegacyAdminCookieOnEveryAdminDataEndpoint() throws Exception {
        String token = tokenService.generateToken(1L, "legacy-admin", UserRole.ADMIN);
        for (String endpoint : new String[]{"summary", "users", "meetings", "usage", "auth/session"}) {
            mockMvc.perform(get("/api/v1/admin/" + endpoint).cookie(new Cookie("__session", token)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void shouldRejectNonAllowedGoogleAccountAndInvalidSignature() throws Exception {
        when(adminGoogleJwtDecoder.decode("outsider")).thenReturn(identity("outsider@gmail.com"));
        when(adminGoogleJwtDecoder.decode("forged")).thenThrow(new BadJwtException("bad signature"));
        for (String token : new String[]{"outsider", "forged"}) {
            mockMvc.perform(get("/api/v1/admin/summary").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void shouldExposeOnlyPublicLoginConfigurationWithoutCaching() throws Exception {
        mockMvc.perform(get("/api/v1/admin/auth/config"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.clientId").value("admin-test-client"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void exchangesGoogleLoginAndRestoresLongSessionWithoutRenewingIt() throws Exception {
        when(adminGoogleJwtDecoder.decode("google-token")).thenReturn(identity("owner@gmail.com"));
        var result = mockMvc.perform(post("/api/v1/admin/auth/session").header("Authorization", "Bearer google-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identity.email").value("owner@gmail.com"))
                .andReturn();
        var data = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString()).get("data");
        String credential = data.get("credential").asText();
        org.assertj.core.api.Assertions.assertThat(Instant.parse(data.get("identity").get("expiresAt").asText()))
                .isAfter(Instant.now().plusSeconds(29 * 86400));
        mockMvc.perform(get("/api/v1/admin/auth/session").header("Authorization", "Bearer " + credential))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.subject").value("google-owner-id"));
        mockMvc.perform(post("/api/v1/admin/auth/session").header("Authorization", "Bearer " + credential))
                .andExpect(status().isUnauthorized());
        String ordinaryToken = tokenService.generateToken(1L, "legacy-admin", UserRole.ADMIN);
        mockMvc.perform(post("/api/v1/admin/auth/session").header("Authorization", "Bearer " + ordinaryToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/admin/auth/session")).andExpect(status().isUnauthorized());
    }

    private Jwt identity(String email) {
        return Jwt.withTokenValue("google-token").header("alg", "RS256")
                .issuer("https://accounts.google.com").subject("google-owner-id")
                .audience(java.util.List.of("admin-test-client"))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .claim("email", email).claim("email_verified", true).build();
    }
}

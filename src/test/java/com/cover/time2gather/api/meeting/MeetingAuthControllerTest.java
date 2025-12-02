package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.meeting.dto.request.AnonymousLoginRequest;
import com.cover.time2gather.config.JpaAuditingConfig;
import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.auth.service.AnonymousLoginResult;
import com.cover.time2gather.domain.auth.service.AnonymousLoginService;
import com.cover.time2gather.domain.auth.service.InvalidPasswordException;
import com.cover.time2gather.infra.oauth.OidcProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = MeetingAuthController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
@AutoConfigureMockMvc(addFilters = false)
class MeetingAuthControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnonymousLoginService anonymousLoginService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private OidcProviderRegistry oidcProviderRegistry;

    @Test
    void shouldLoginAnonymousUserSuccessfully() throws Exception {
        // Given
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String password = "1234";

        AnonymousLoginRequest request = new AnonymousLoginRequest(username, password);

        AnonymousLoginResult loginResult = new AnonymousLoginResult(
                "jwt-token",
                true,
                1L,
                "철수"
        );

        when(anonymousLoginService.login(eq(meetingCode), eq(username), eq(password)))
                .thenReturn(loginResult);

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("철수"))
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().httpOnly("accessToken", true));
    }

    @Test
    void shouldLoginExistingAnonymousUser() throws Exception {
        // Given
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String password = "1234";

        AnonymousLoginRequest request = new AnonymousLoginRequest(username, password);

        AnonymousLoginResult loginResult = new AnonymousLoginResult(
                "jwt-token",
                false,  // 기존 유저
                1L,
                "철수"
        );

        when(anonymousLoginService.login(eq(meetingCode), eq(username), eq(password)))
                .thenReturn(loginResult);

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isNewUser").value(false))
                .andExpect(cookie().exists("accessToken"));
    }

    @Test
    void shouldReturn401WhenPasswordIncorrect() throws Exception {
        // Given
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String password = "wrong-password";

        AnonymousLoginRequest request = new AnonymousLoginRequest(username, password);

        when(anonymousLoginService.login(eq(meetingCode), eq(username), eq(password)))
                .thenThrow(new InvalidPasswordException("Invalid password"));

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn400WhenUsernameEmpty() throws Exception {
        // Given
        String meetingCode = "mtg_abc123";
        AnonymousLoginRequest request = new AnonymousLoginRequest("", "1234");

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPasswordEmpty() throws Exception {
        // Given
        String meetingCode = "mtg_abc123";
        AnonymousLoginRequest request = new AnonymousLoginRequest("철수", "");

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}


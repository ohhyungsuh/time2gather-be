package com.cover.time2gather.api.auth;

import com.cover.time2gather.api.auth.dto.request.OAuthLoginRequest;
import com.cover.time2gather.config.JpaAuditingConfig;
import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.auth.service.OAuthLoginResult;
import com.cover.time2gather.domain.auth.service.OAuthLoginService;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.oauth.OidcProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OAuthLoginService oAuthLoginService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private OidcProviderRegistry oidcProviderRegistry;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private MeetingRepository meetingRepository;

    @Test
    @WithMockUser
    void shouldLoginWithKakaoAuthorizationCode() throws Exception {
        // Given
        String provider = "kakao";
        String authCode = "test-auth-code";

        OAuthLoginRequest request = new OAuthLoginRequest();
        request.setAuthorizationCode(authCode);

        OAuthLoginResult loginResult = new OAuthLoginResult(
                "jwt-token",
                true,
                1L,
                "kakao_12345",
                "user@kakao.com",
                "https://example.com/profile.jpg",
                null
        );

        when(oAuthLoginService.login(eq(provider), eq(authCode), any())).thenReturn(loginResult);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/oauth/{provider}", provider)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("kakao_12345"))
                .andExpect(jsonPath("$.data.email").value("user@kakao.com"))
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().secure("accessToken", true)); // secure 쿠키 설정
    }
}


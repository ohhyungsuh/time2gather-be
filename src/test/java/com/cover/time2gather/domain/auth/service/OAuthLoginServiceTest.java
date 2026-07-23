package com.cover.time2gather.domain.auth.service;

import com.cover.time2gather.config.security.AdminAccountPolicy;
import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.infra.oauth.OidcProviderRegistry;
import com.cover.time2gather.infra.oauth.OidcProviderStrategy;
import com.cover.time2gather.infra.oauth.OidcUserInfo;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRole;
import com.cover.time2gather.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceTest {

    @Mock
    private OidcProviderRegistry providerRegistry;

    @Mock
    private OidcProviderStrategy kakaoProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AdminAccountPolicy adminAccountPolicy;

    private OAuthLoginService oAuthLoginService;

    @BeforeEach
    void setUp() {
        oAuthLoginService = new OAuthLoginService(
                providerRegistry,
                userRepository,
                jwtTokenService,
                adminAccountPolicy
        );
    }

    @Test
    void shouldLoginWithKakaoAndCreateNewUser() {
        // Given
        String provider = "kakao";
        String authCode = "test-auth-code";
        String redirectUri = "http://localhost:3000/callback";

        OidcUserInfo userInfo = new OidcUserInfo(
                "id-token",
                "12345",
                "user@kakao.com",
                "test_nickname",
                "http://profile.url",
                true
        );

        when(providerRegistry.getProvider(provider)).thenReturn(kakaoProvider);
        when(kakaoProvider.getUserInfo(anyString(), anyString())).thenReturn(userInfo);
        when(userRepository.findByProviderAndProviderId(User.AuthProvider.KAKAO, "12345"))
                .thenReturn(Optional.empty());

        User savedUser = User.builder()
                .username("test_nickname")
                .email("user@kakao.com")
                .provider(User.AuthProvider.KAKAO)
                .providerId("12345")
                .profileImageUrl("http://profile.url")
                .build();
        ReflectionTestUtils.setField(savedUser, "id", 1L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenService.generateToken(anyLong(), anyString(), any())).thenReturn("jwt-token");

        // When
        OAuthLoginResult result = oAuthLoginService.login(provider, authCode, redirectUri);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJwtToken()).isEqualTo("jwt-token");
        assertThat(result.isNewUser()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getProvider()).isEqualTo(User.AuthProvider.KAKAO);
        assertThat(capturedUser.getProviderId()).isEqualTo("12345");
        assertThat(capturedUser.getUsername()).isEqualTo("test_nickname");
    }

    @Test
    void shouldLoginWithExistingUser() {
        // Given
        String provider = "kakao";
        String authCode = "test-auth-code";
        String redirectUri = "http://localhost:3000/callback";

        OidcUserInfo userInfo = new OidcUserInfo(
                "id-token",
                "12345",
                "user@kakao.com",
                "test_nickname",
                "http://profile.url",
                true
        );

        User existingUser = User.builder()
                .username("test_nickname")
                .email("user@kakao.com")
                .provider(User.AuthProvider.KAKAO)
                .providerId("12345")
                .profileImageUrl("http://old.url")
                .build();
        ReflectionTestUtils.setField(existingUser, "id", 1L);

        when(providerRegistry.getProvider(provider)).thenReturn(kakaoProvider);
        when(kakaoProvider.getUserInfo(anyString(), anyString())).thenReturn(userInfo);
        when(userRepository.findByProviderAndProviderId(User.AuthProvider.KAKAO, "12345"))
                .thenReturn(Optional.of(existingUser));
        when(jwtTokenService.generateToken(anyLong(), anyString(), any())).thenReturn("jwt-token");

        // When
        OAuthLoginResult result = oAuthLoginService.login(provider, authCode, redirectUri);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJwtToken()).isEqualTo("jwt-token");
        assertThat(result.isNewUser()).isFalse();

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldPromoteConfiguredKakaoAccountBeforeIssuingJwt() {
        String provider = "kakao";
        String adminEmail = "admin@example.com";
        OidcUserInfo userInfo = new OidcUserInfo(
                "id-token",
                "12345",
                adminEmail,
                "admin",
                "http://profile.url",
                true
        );
        User existingUser = User.builder()
                .username("admin")
                .email(adminEmail)
                .provider(User.AuthProvider.KAKAO)
                .providerId("12345")
                .build();
        ReflectionTestUtils.setField(existingUser, "id", 1L);

        when(providerRegistry.getProvider(provider)).thenReturn(kakaoProvider);
        when(kakaoProvider.getUserInfo(anyString(), anyString())).thenReturn(userInfo);
        when(userRepository.findByProviderAndProviderId(User.AuthProvider.KAKAO, "12345"))
                .thenReturn(Optional.of(existingUser));
        when(adminAccountPolicy.isAdminAccount(User.AuthProvider.KAKAO, adminEmail, true)).thenReturn(true);
        when(jwtTokenService.generateToken(1L, "admin", UserRole.ADMIN)).thenReturn("admin-jwt");

        OAuthLoginResult result = oAuthLoginService.login(provider, "auth-code", "redirect-uri");

        assertThat(result.getJwtToken()).isEqualTo("admin-jwt");
        assertThat(result.getUser().getRole()).isEqualTo(UserRole.ADMIN);
        verify(jwtTokenService).generateToken(1L, "admin", UserRole.ADMIN);
    }
}

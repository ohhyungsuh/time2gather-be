package com.cover.time2gather.domain.auth.service;

import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.auth.oidc.OidcProviderRegistry;
import com.cover.time2gather.domain.auth.oidc.OidcProviderStrategy;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private OAuthLoginService oAuthLoginService;

    @BeforeEach
    void setUp() {
        oAuthLoginService = new OAuthLoginService(providerRegistry, userRepository, jwtTokenService);
    }

    @Test
    void shouldLoginWithKakaoAndCreateNewUser() {
        // Given
        String provider = "kakao";
        String authCode = "test-auth-code";
        String idToken = createMockIdToken("12345", "kakao_user", "user@kakao.com");

        when(providerRegistry.getProvider(provider)).thenReturn(kakaoProvider);
        when(kakaoProvider.getIdToken(authCode)).thenReturn(idToken);
        when(userRepository.findByProviderAndProviderId(User.AuthProvider.KAKAO, "12345"))
                .thenReturn(Optional.empty());

        User savedUser = User.builder()
                .username("kakao_12345")
                .email("user@kakao.com")
                .provider(User.AuthProvider.KAKAO)
                .providerId("12345")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenService.generateToken(any(), any())).thenReturn("jwt-token");

        // When
        OAuthLoginResult result = oAuthLoginService.login(provider, authCode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJwtToken()).isEqualTo("jwt-token");
        assertThat(result.isNewUser()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getProvider()).isEqualTo(User.AuthProvider.KAKAO);
        assertThat(capturedUser.getProviderId()).isEqualTo("12345");
    }

    @Test
    void shouldLoginWithExistingUser() {
        // Given
        String provider = "kakao";
        String authCode = "test-auth-code";
        String idToken = createMockIdToken("12345", "kakao_user", "user@kakao.com");

        User existingUser = User.builder()
                .username("kakao_12345")
                .email("user@kakao.com")
                .provider(User.AuthProvider.KAKAO)
                .providerId("12345")
                .build();

        when(providerRegistry.getProvider(provider)).thenReturn(kakaoProvider);
        when(kakaoProvider.getIdToken(authCode)).thenReturn(idToken);
        when(userRepository.findByProviderAndProviderId(User.AuthProvider.KAKAO, "12345"))
                .thenReturn(Optional.of(existingUser));
        when(jwtTokenService.generateToken(any(), any())).thenReturn("jwt-token");

        // When
        OAuthLoginResult result = oAuthLoginService.login(provider, authCode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJwtToken()).isEqualTo("jwt-token");
        assertThat(result.isNewUser()).isFalse();

        verify(userRepository, never()).save(any());
    }

    private String createMockIdToken(String sub, String nickname, String email) {
        // Simple mock ID token: header.payload.signature
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString(
                String.format("{\"sub\":\"%s\",\"nickname\":\"%s\",\"email\":\"%s\"}", sub, nickname, email).getBytes()
        );
        String signature = Base64.getUrlEncoder().encodeToString("mock-signature".getBytes());
        return header + "." + payload + "." + signature;
    }
}


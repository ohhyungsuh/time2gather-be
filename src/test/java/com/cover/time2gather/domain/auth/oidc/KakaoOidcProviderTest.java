package com.cover.time2gather.infra.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoOidcProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private KakaoOidcProvider kakaoOidcProvider;

    @BeforeEach
    void setUp() {
        String clientId = "test-client-id";
        String clientSecret = "test-client-secret";
        String redirectUri = "http://localhost:3000/callback";
        kakaoOidcProvider = new KakaoOidcProvider(restTemplate, clientId, clientSecret);
    }

    @Test
    void shouldReturnKakaoAsProviderName() {
        // When
        String providerName = kakaoOidcProvider.getProviderName();

        // Then
        assertThat(providerName).isEqualTo("kakao");
    }

    @Test
    void shouldGetIdTokenFromKakaoWithAuthorizationCode() {
        // Given
        String authorizationCode = "test-auth-code";
        String expectedIdToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

        // Kakao Token Response 모의
        var tokenResponse = new KakaoTokenResponse();
        tokenResponse.setIdToken(expectedIdToken);
        tokenResponse.setAccessToken("test-access-token");
        tokenResponse.setTokenType("bearer");
        tokenResponse.setExpiresIn(3600);

        when(restTemplate.postForObject(
                eq("https://kauth.kakao.com/oauth/token"),
                any(),
                eq(KakaoTokenResponse.class)
        )).thenReturn(tokenResponse);

        // When
        String idToken = kakaoOidcProvider.getIdToken(authorizationCode, null);

        // Then
        assertThat(idToken).isEqualTo(expectedIdToken);
    }
}


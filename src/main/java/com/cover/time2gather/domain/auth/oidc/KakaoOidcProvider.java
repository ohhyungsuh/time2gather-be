package com.cover.time2gather.domain.auth.oidc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Kakao OIDC Provider 구현
 */
@Slf4j
@RequiredArgsConstructor
public class KakaoOidcProvider implements OidcProviderStrategy {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    @Override
    public String getIdToken(String authorizationCode) {
        log.info("Requesting ID token from Kakao with authorization code");
        log.debug("Client ID: {}, Redirect URI: {}", clientId, redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", authorizationCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            KakaoTokenResponse response = restTemplate.postForObject(
                    KAKAO_TOKEN_URL,
                    request,
                    KakaoTokenResponse.class
            );

            if (response == null || response.getIdToken() == null) {
                log.error("Failed to get ID token from Kakao: response is null or missing id_token");
                throw new IllegalStateException("Failed to get ID token from Kakao");
            }

            log.info("Successfully obtained ID token from Kakao");
            return response.getIdToken();

        } catch (HttpClientErrorException e) {
            log.error("Kakao API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == 401) {
                throw new IllegalArgumentException(
                    "Invalid authorization code or expired. Please re-authenticate with Kakao. " +
                    "Error: " + e.getResponseBodyAsString()
                );
            }

            throw new IllegalStateException(
                "Failed to communicate with Kakao API: " + e.getMessage(),
                e
            );
        } catch (Exception e) {
            log.error("Unexpected error while getting ID token from Kakao", e);
            throw new IllegalStateException("Failed to get ID token from Kakao: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "kakao";
    }
}


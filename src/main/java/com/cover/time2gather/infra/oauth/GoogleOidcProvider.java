package com.cover.time2gather.infra.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Google OIDC Provider 구현
 */
@Slf4j
@RequiredArgsConstructor
public class GoogleOidcProvider implements OidcProviderStrategy {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private static final String GRANT_TYPE = "authorization_code";
    private static final String PARAM_GRANT_TYPE = "grant_type";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_CLIENT_SECRET = "client_secret";
    private static final String PARAM_REDIRECT_URI = "redirect_uri";
    private static final String PARAM_CODE = "code";

    private static final String ID_KEY = "id";
    private static final String EMAIL_KEY = "email";
    private static final String NAME_KEY = "name";
    private static final String PICTURE_KEY = "picture";

    private static final int HTTP_STATUS_UNAUTHORIZED = 401;
    private static final int HTTP_STATUS_BAD_REQUEST = 400;
    private static final String ERROR_MESSAGE_INVALID_CODE = "Invalid authorization code or expired. Please re-authenticate with Google. Error: %s";
    private static final String ERROR_MESSAGE_API_FAILURE = "Failed to communicate with Google API: %s";
    private static final String ERROR_MESSAGE_NO_ID_TOKEN = "Failed to get ID token from Google";
    private static final String ERROR_MESSAGE_NO_USER_INFO = "Failed to get user info from Google";
    private static final String PROVIDER_NAME = "google";

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;

    @Override
    public String getIdToken(String authorizationCode, String redirectUri) {
        log.info("Requesting ID token from Google with authorization code");
        log.debug("Client ID: {}, Redirect URI: {}", clientId, redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(PARAM_GRANT_TYPE, GRANT_TYPE);
        params.add(PARAM_CLIENT_ID, clientId);
        params.add(PARAM_CLIENT_SECRET, clientSecret);
        params.add(PARAM_REDIRECT_URI, redirectUri);
        params.add(PARAM_CODE, authorizationCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            GoogleTokenResponse response = restTemplate.postForObject(
                    GOOGLE_TOKEN_URL,
                    request,
                    GoogleTokenResponse.class
            );

            if (response == null || response.getIdToken() == null) {
                log.error(ERROR_MESSAGE_NO_ID_TOKEN + ": response is null or missing id_token");
                throw new IllegalStateException(ERROR_MESSAGE_NO_ID_TOKEN);
            }

            log.info("Successfully obtained ID token from Google");

            return response.getIdToken();

        } catch (HttpClientErrorException e) {
            log.error("Google API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == HTTP_STATUS_UNAUTHORIZED ||
                e.getStatusCode().value() == HTTP_STATUS_BAD_REQUEST) {
                throw new IllegalArgumentException(
                    String.format(ERROR_MESSAGE_INVALID_CODE, e.getResponseBodyAsString())
                );
            }

            throw new IllegalStateException(
                String.format(ERROR_MESSAGE_API_FAILURE, e.getMessage()),
                e
            );
        } catch (Exception e) {
            log.error("Unexpected error while getting ID token from Google", e);
            throw new IllegalStateException(ERROR_MESSAGE_NO_ID_TOKEN + ": " + e.getMessage(), e);
        }
    }

    @Override
    public OidcUserInfo getUserInfo(String authorizationCode, String redirectUri) {
        log.info("Requesting user info from Google with authorization code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(PARAM_GRANT_TYPE, GRANT_TYPE);
        params.add(PARAM_CLIENT_ID, clientId);
        params.add(PARAM_CLIENT_SECRET, clientSecret);
        params.add(PARAM_REDIRECT_URI, redirectUri);
        params.add(PARAM_CODE, authorizationCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            // 1. 토큰 요청
            GoogleTokenResponse tokenResponse = restTemplate.postForObject(
                    GOOGLE_TOKEN_URL,
                    request,
                    GoogleTokenResponse.class
            );

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                log.error(ERROR_MESSAGE_NO_ID_TOKEN + ": response is null or missing access_token");
                throw new IllegalStateException(ERROR_MESSAGE_NO_ID_TOKEN);
            }

            log.info("Successfully obtained access token from Google");

            // 2. Access Token을 사용하여 사용자 정보 조회
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(tokenResponse.getAccessToken());

            HttpEntity<Void> userInfoRequest = new HttpEntity<>(userInfoHeaders);

            ResponseEntity<Map<String, Object>> userInfoResponse = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL,
                    HttpMethod.GET,
                    userInfoRequest,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (userInfoResponse.getBody() == null) {
                log.error(ERROR_MESSAGE_NO_USER_INFO + ": response body is null");
                throw new IllegalStateException(ERROR_MESSAGE_NO_USER_INFO);
            }

            Map<String, Object> userInfo = userInfoResponse.getBody();
            
            String providerId = String.valueOf(userInfo.get(ID_KEY));
            String email = (String) userInfo.get(EMAIL_KEY);
            String name = (String) userInfo.get(NAME_KEY);
            String profileImageUrl = (String) userInfo.get(PICTURE_KEY);

            log.info("Successfully fetched user info from Google - providerId: {}, email: {}, name: {}",
                    providerId, email, name);

            return new OidcUserInfo(
                    tokenResponse.getIdToken(),
                    providerId,
                    email,
                    name,
                    profileImageUrl
            );

        } catch (HttpClientErrorException e) {
            log.error("Google API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == HTTP_STATUS_UNAUTHORIZED ||
                e.getStatusCode().value() == HTTP_STATUS_BAD_REQUEST) {
                throw new IllegalArgumentException(
                        String.format(ERROR_MESSAGE_INVALID_CODE, e.getResponseBodyAsString())
                );
            }

            throw new IllegalStateException(
                    String.format(ERROR_MESSAGE_API_FAILURE, e.getMessage()),
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error while getting user info from Google", e);
            throw new IllegalStateException(ERROR_MESSAGE_NO_USER_INFO + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}

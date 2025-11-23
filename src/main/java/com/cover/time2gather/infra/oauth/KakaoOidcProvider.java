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
 * Kakao OIDC Provider 구현
 */
@Slf4j
@RequiredArgsConstructor
public class KakaoOidcProvider implements OidcProviderStrategy {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private static final String GRANT_TYPE = "authorization_code";
    private static final String PARAM_GRANT_TYPE = "grant_type";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_CLIENT_SECRET = "client_secret";
    private static final String PARAM_REDIRECT_URI = "redirect_uri";
    private static final String PARAM_CODE = "code";

    private static final String KAKAO_ACCOUNT_KEY = "kakao_account";
    private static final String PROPERTIES_KEY = "properties";
    private static final String PROFILE_KEY = "profile";
    private static final String ID_KEY = "id";
    private static final String EMAIL_KEY = "email";
    private static final String NICKNAME_KEY = "nickname";
    private static final String PROFILE_IMAGE_URL_KEY = "profile_image_url";
    private static final String THUMBNAIL_IMAGE_URL_KEY = "thumbnail_image_url";
    private static final String PROFILE_IMAGE_KEY = "profile_image";
    private static final String THUMBNAIL_IMAGE_KEY = "thumbnail_image";

    private static final int HTTP_STATUS_UNAUTHORIZED = 401;
    private static final String ERROR_MESSAGE_INVALID_CODE = "Invalid authorization code or expired. Please re-authenticate with Kakao. Error: %s";
    private static final String ERROR_MESSAGE_API_FAILURE = "Failed to communicate with Kakao API: %s";
    private static final String ERROR_MESSAGE_NO_ID_TOKEN = "Failed to get ID token from Kakao";
    private static final String ERROR_MESSAGE_NO_USER_INFO = "Failed to get user info from Kakao";
    private static final String PROVIDER_NAME = "kakao";

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
        params.add(PARAM_GRANT_TYPE, GRANT_TYPE);
        params.add(PARAM_CLIENT_ID, clientId);
        params.add(PARAM_CLIENT_SECRET, clientSecret);
        params.add(PARAM_REDIRECT_URI, redirectUri);
        params.add(PARAM_CODE, authorizationCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            KakaoTokenResponse response = restTemplate.postForObject(
                    KAKAO_TOKEN_URL,
                    request,
                    KakaoTokenResponse.class
            );

            if (response == null || response.getIdToken() == null) {
                log.error(ERROR_MESSAGE_NO_ID_TOKEN + ": response is null or missing id_token");
                throw new IllegalStateException(ERROR_MESSAGE_NO_ID_TOKEN);
            }

            log.info("Successfully obtained ID token from Kakao");

            return response.getIdToken();

        } catch (HttpClientErrorException e) {
            log.error("Kakao API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == HTTP_STATUS_UNAUTHORIZED) {
                throw new IllegalArgumentException(
                    String.format(ERROR_MESSAGE_INVALID_CODE, e.getResponseBodyAsString())
                );
            }

            throw new IllegalStateException(
                String.format(ERROR_MESSAGE_API_FAILURE, e.getMessage()),
                e
            );
        } catch (Exception e) {
            log.error("Unexpected error while getting ID token from Kakao", e);
            throw new IllegalStateException(ERROR_MESSAGE_NO_ID_TOKEN + ": " + e.getMessage(), e);
        }
    }

    @Override
    public OidcUserInfo getUserInfo(String authorizationCode) {
        log.info("Requesting user info from Kakao with authorization code");

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
            KakaoTokenResponse tokenResponse = restTemplate.postForObject(
                    KAKAO_TOKEN_URL,
                    request,
                    KakaoTokenResponse.class
            );

            if (tokenResponse == null || tokenResponse.getIdToken() == null) {
                log.error(ERROR_MESSAGE_NO_ID_TOKEN + ": response is null or missing id_token");
                throw new IllegalStateException(ERROR_MESSAGE_NO_ID_TOKEN);
            }

            log.info("Successfully obtained ID token from Kakao");

            // 2. Access Token을 사용하여 사용자 정보 조회
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(tokenResponse.getAccessToken());

            HttpEntity<Void> userInfoRequest = new HttpEntity<>(userInfoHeaders);

            ResponseEntity<Map<String, Object>> userInfoResponse = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
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

            // kakao_account에서 이메일 추출
            String email = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get(KAKAO_ACCOUNT_KEY);
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get(EMAIL_KEY);
            }

            // properties 또는 kakao_account.profile에서 닉네임과 프로필 이미지 추출
            String nickname = null;
            String profileImageUrl = null;

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) userInfo.get(PROPERTIES_KEY);
            if (properties != null) {
                nickname = (String) properties.get(NICKNAME_KEY);
            }

            if (kakaoAccount != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get(PROFILE_KEY);
                if (profile != null) {
                    if (nickname == null) {
                        nickname = (String) profile.get(NICKNAME_KEY);
                    }
                    profileImageUrl = (String) profile.get(PROFILE_IMAGE_URL_KEY);
                    if (profileImageUrl == null) {
                        profileImageUrl = (String) profile.get(THUMBNAIL_IMAGE_URL_KEY);
                    }
                }
            }

            // properties에서도 프로필 이미지 확인
            if (profileImageUrl == null && properties != null) {
                profileImageUrl = (String) properties.get(PROFILE_IMAGE_KEY);
                if (profileImageUrl == null) {
                    profileImageUrl = (String) properties.get(THUMBNAIL_IMAGE_KEY);
                }
            }

            log.info("Successfully fetched user info from Kakao - providerId: {}, email: {}, profileImageUrl: {}",
                    providerId, email, profileImageUrl);

            return new OidcUserInfo(
                    tokenResponse.getIdToken(),
                    providerId,
                    email,
                    nickname,
                    profileImageUrl
            );

        } catch (HttpClientErrorException e) {
            log.error("Kakao API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == HTTP_STATUS_UNAUTHORIZED) {
                throw new IllegalArgumentException(
                        String.format(ERROR_MESSAGE_INVALID_CODE, e.getResponseBodyAsString())
                );
            }

            throw new IllegalStateException(
                    String.format(ERROR_MESSAGE_API_FAILURE, e.getMessage()),
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error while getting user info from Kakao", e);
            throw new IllegalStateException(ERROR_MESSAGE_NO_USER_INFO + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}


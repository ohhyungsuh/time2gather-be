package com.cover.time2gather.infra.oauth;

/**
 * OIDC Provider 전략 인터페이스
 * 다양한 OAuth2/OIDC Provider(Kakao, Google 등)를 지원하기 위한 전략 패턴
 */
public interface OidcProviderStrategy {

    /**
     * Authorization Code를 사용해서 ID Token을 획득
     *
     * @param authorizationCode OAuth2 Authorization Code
     * @return ID Token (JWT)
     */
    String getIdToken(String authorizationCode);

    /**
     * Authorization Code를 사용하여 사용자 정보를 획득
     *
     * @param authorizationCode OAuth2 Authorization Code
     * @return 사용자 정보
     */
    OidcUserInfo getUserInfo(String authorizationCode);

    /**
     * Provider 이름 반환
     *
     * @return provider 이름 (예: "kakao", "google")
     */
    String getProviderName();
}


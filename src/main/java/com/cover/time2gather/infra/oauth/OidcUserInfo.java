package com.cover.time2gather.infra.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OIDC Provider로부터 받은 사용자 정보
 */
@Getter
@AllArgsConstructor
public class OidcUserInfo {
    private final String idToken;
    private final String providerId;
    private final String email;
    private final String nickname;
    private final String profileImageUrl;
}


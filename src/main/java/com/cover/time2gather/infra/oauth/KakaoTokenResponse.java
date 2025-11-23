package com.cover.time2gather.infra.oauth;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class KakaoTokenResponse {

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;

    // 사용자 정보 API에서 가져온 프로필 이미지 URL (JSON 응답에는 없음)
    private String profileImageUrl;
}


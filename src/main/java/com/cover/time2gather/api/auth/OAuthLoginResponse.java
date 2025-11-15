package com.cover.time2gather.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "OAuth 로그인 응답")
@Getter
@RequiredArgsConstructor
public class OAuthLoginResponse {

    @Schema(description = "사용자 ID", example = "1")
    private final Long userId;

    @Schema(description = "사용자 이름", example = "kakao_1234567890")
    private final String username;

    @Schema(description = "이메일", example = "user@kakao.com")
    private final String email;

    @Schema(description = "OAuth Provider", example = "kakao")
    private final String provider;

    @Schema(description = "신규 사용자 여부", example = "true")
    @JsonProperty("isNewUser")
    private final boolean isNewUser;
}

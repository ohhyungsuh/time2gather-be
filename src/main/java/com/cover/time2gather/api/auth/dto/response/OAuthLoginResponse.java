package com.cover.time2gather.api.auth.dto.response;

import com.cover.time2gather.domain.auth.service.OAuthLoginResult;
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

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private final String profileImageUrl;

    @Schema(description = "OAuth Provider", example = "kakao")
    private final String provider;

    @Schema(description = "신규 사용자 여부", example = "true")
    @JsonProperty("isNewUser")
    private final Boolean isNewUser;

    /**
     * 도메인 결과로부터 응답 DTO 생성
     * DTO 변환 책임을 DTO 자신이 담당 (Single Responsibility Principle)
     *
     * @param loginResult OAuth 로그인 도메인 결과
     * @param provider OAuth 제공자명
     * @return OAuthLoginResponse 인스턴스
     */
    public static OAuthLoginResponse from(OAuthLoginResult loginResult, String provider) {
        return new OAuthLoginResponse(
                loginResult.getUserId(),
                loginResult.getUsername(),
                loginResult.getEmail(),
                loginResult.getProfileImageUrl(),
                provider,
                loginResult.isNewUser()
        );
    }
}

package com.cover.time2gather.api.auth;

import com.cover.time2gather.domain.auth.service.OAuthLoginResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "테스트용 토큰 생성 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestTokenResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자 이름", example = "test_user")
    private String username;

    @Schema(description = "JWT Access Token (Bearer 토큰으로 사용)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "토큰 만료 시간 (밀리초)", example = "3600000")
    private Long expiresIn;

    public static TestTokenResponse from(OAuthLoginResult loginResult) {
        return TestTokenResponse.builder()
                .userId(loginResult.getUser().getId())
                .username(loginResult.getUser().getUsername())
                .accessToken(loginResult.getJwtToken())
                .expiresIn(3600000L) // 1시간
                .build();
    }
}


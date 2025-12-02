package com.cover.time2gather.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "OAuth 로그인 요청")
@Getter
@Setter
@NoArgsConstructor
public class OAuthLoginRequest {

    @Schema(description = "OAuth Authorization Code",
            example = "0jVtEEMB9eJtGR-SUnkEbCazKEGk3XFZAAAAAQoXIS0AAAGahuf3ZLfuZLkpz6yP")
    @NotBlank(message = "인가 코드는 필수입니다")
    private String authorizationCode;

    @Schema(description = "Redirect URL (선택). 미입력시 기본 설정값 사용",
            example = "http://localhost:3000/auth/callback")
    private String redirectUrl;
}



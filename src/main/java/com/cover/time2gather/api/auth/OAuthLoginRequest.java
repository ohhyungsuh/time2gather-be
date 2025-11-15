package com.cover.time2gather.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "OAuth 로그인 요청")
@Getter
@NoArgsConstructor
public class OAuthLoginRequest {

    @Schema(description = "OAuth Authorization Code", example = "0jVtEEMB9eJtGR-SUnkEbCazKEGk3XFZAAAAAQoXIS0AAAGahuf3ZLfuZLkpz6yP", required = true)
    private String authorizationCode;

    public OAuthLoginRequest(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }
}

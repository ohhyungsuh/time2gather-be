package com.cover.time2gather.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "OAuth login request")
@Getter
@Setter
@NoArgsConstructor
public class OAuthLoginRequest {

    @Schema(description = "OAuth Authorization Code",
            example = "0jVtEEMB9eJtGR-SUnkEbCazKEGk3XFZAAAAAQoXIS0AAAGahuf3ZLfuZLkpz6yP")
    @NotBlank(message = "{validation.auth.code.required}")
    private String authorizationCode;

    @Schema(description = "Redirect URL (optional). Uses default if not provided",
            example = "http://localhost:3000/auth/callback")
    private String redirectUrl;
}



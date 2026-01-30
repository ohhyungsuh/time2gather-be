package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Anonymous login request")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousLoginRequest {

    @NotBlank(message = "{validation.username.required}")
    @Schema(description = "Username (unique within the meeting scope)", example = "John", required = true)
    private String username;

    @NotBlank(message = "{validation.password.required}")
    @Schema(description = "Password", example = "1234", required = true)
    private String password;
}

package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Anonymous login request")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousLoginRequest {

    @NotBlank(message = "{validation.username.required}")
    @Size(max = 100)
    @Schema(description = "Display name (not unique)", example = "John", required = true)
    private String username;
}

package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Confirm location request")
public class ConfirmLocationRequest {

    @NotNull(message = "{validation.location.id.required}")
    @Schema(description = "Location ID to confirm", example = "1")
    private Long locationId;
}

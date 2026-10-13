package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update the exact time of a confirmed meeting")
public class UpdateConfirmedTimeRequest {

    @NotNull(message = "{validation.time.required}")
    @Schema(description = "Confirmed meeting time", example = "18:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime time;
}

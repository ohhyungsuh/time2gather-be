package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Confirm meeting schedule request")
public class ConfirmMeetingRequest {

    @NotNull(message = "{validation.date.required}")
    @Schema(description = "Date to confirm", example = "2024-02-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @Schema(description = "Slot index to confirm (required for TIME type, null for ALL_DAY type)", example = "28")
    private Integer slotIndex;
}

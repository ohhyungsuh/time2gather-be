package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Calendar export request")
public class ExportCalendarRequest {

    @NotBlank(message = "{validation.date.required}")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "{validation.date.format}")
    @Schema(description = "Event date", example = "2024-02-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String date;

    @NotBlank(message = "{validation.time.required}")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "{validation.time.format}")
    @Schema(description = "Event time", example = "14:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private String time;
}


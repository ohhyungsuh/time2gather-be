package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update meeting voting dates request")
public class UpdateAvailableDatesRequest {

    @NotNull(message = "{validation.meeting.dates.required}")
    @Size(min = 1, max = 31, message = "{validation.selection.date.min.max}")
    @Schema(description = "Voting dates in yyyy-MM-dd format", example = "[\"2026-10-16\", \"2026-10-17\"]")
    private List<String> dates;

    @Size(min = 1, max = 1440)
    @Schema(description = "Optional voting times (HH:mm), applied to every selected date. Omit to preserve existing times.")
    private List<@NotNull @Pattern(regexp = "(?:[01][0-9]|2[0-3]):[0-5][0-9]", message = "{validation.time.format}") String> times;
}

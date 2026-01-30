package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vote locations request")
public class VoteLocationsRequest {

    @NotNull(message = "{validation.location.ids.required}")
    @Schema(description = "List of location IDs to vote for (empty array to skip voting)", example = "[1, 2]")
    private List<Long> locationIds;
}

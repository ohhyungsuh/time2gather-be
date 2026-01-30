package com.cover.time2gather.api.meeting.dto.response;

import com.cover.time2gather.domain.meeting.MeetingLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Location response")
public class LocationResponse {

    @Schema(description = "Location ID", example = "1")
    private Long id;

    @Schema(description = "Location name", example = "Gangnam Station Starbucks")
    private String name;

    @Schema(description = "Display order", example = "0")
    private int displayOrder;

    public static LocationResponse from(MeetingLocation location) {
        return new LocationResponse(
            location.getId(),
            location.getName(),
            location.getDisplayOrder()
        );
    }
}

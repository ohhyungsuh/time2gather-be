package com.cover.time2gather.api.meeting.dto.response;

import com.cover.time2gather.domain.meeting.MeetingLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "장소 응답")
public class LocationResponse {

    @Schema(description = "장소 ID", example = "1")
    private Long id;

    @Schema(description = "장소 이름", example = "강남역 스타벅스")
    private String name;

    @Schema(description = "표시 순서", example = "0")
    private int displayOrder;

    public static LocationResponse from(MeetingLocation location) {
        return new LocationResponse(
            location.getId(),
            location.getName(),
            location.getDisplayOrder()
        );
    }
}

package com.cover.time2gather.api.meeting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "User location vote response")
public class UserLocationSelectionsResponse {

    @Schema(description = "List of voted location IDs", example = "[1, 2]")
    private List<Long> locationIds;

    public static UserLocationSelectionsResponse from(List<Long> locationIds) {
        return new UserLocationSelectionsResponse(locationIds);
    }
}

package com.cover.time2gather.api.meeting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "사용자의 장소 투표 응답")
public class UserLocationSelectionsResponse {

    @Schema(description = "투표한 장소 ID 목록", example = "[1, 2]")
    private List<Long> locationIds;

    public static UserLocationSelectionsResponse from(List<Long> locationIds) {
        return new UserLocationSelectionsResponse(locationIds);
    }
}

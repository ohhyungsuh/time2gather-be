package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
@Schema(description = "사용자 선택 조회 응답")
public class UserSelectionResponse {

    @Schema(description = "날짜별 선택한 시간대 (HH:mm 형식)",
            example = "{\"2024-02-15\": [\"09:00\", \"09:30\", \"10:30\"], \"2024-02-16\": [\"11:00\", \"11:30\"]}")
    private Map<String, String[]> selections;
}

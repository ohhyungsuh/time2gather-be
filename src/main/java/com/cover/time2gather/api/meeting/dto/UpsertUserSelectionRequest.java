package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 시간 선택 요청")
public class UpsertUserSelectionRequest {

    @NotNull(message = "선택한 시간대는 필수입니다")
    @Schema(description = "날짜별 선택한 시간대 (HH:mm 형식)",
            example = "{\"2024-02-15\": [\"09:00\", \"09:30\", \"10:30\"], \"2024-02-16\": [\"11:00\", \"11:30\"], \"2024-02-17\": []}")
    private Map<String, String[]> selections;
}

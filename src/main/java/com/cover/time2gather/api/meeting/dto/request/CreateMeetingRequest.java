package com.cover.time2gather.api.meeting.dto.request;

import com.cover.time2gather.util.TimeSlotConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "모임 생성 요청")
public class CreateMeetingRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Schema(description = "모임 제목", example = "프로젝트 킥오프 미팅", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "모임 설명 (선택사항)", example = "2월 신규 프로젝트 시작 회의", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    @Schema(description = "타임존 (선택사항, 기본값: Asia/Seoul)", example = "Asia/Seoul", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String timezone;

    @NotNull(message = "가능한 날짜/시간은 필수입니다")
    @Schema(description = "날짜별 가능한 시간대 (HH:mm 형식)",
            example = "{\"2024-02-15\": [\"09:00\", \"09:30\", \"10:00\", \"10:30\"], \"2024-02-16\": [\"11:00\", \"11:30\", \"12:00\"]}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String[]> availableDates;

    /**
     * API "HH:mm" → 도메인 slotIndex 변환
     */
    public Map<String, int[]> toSlotIndexes() {
        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, String[]> entry : availableDates.entrySet()) {
            String date = entry.getKey();
            String[] times = entry.getValue();
            int[] slots = Arrays.stream(times)
                    .mapToInt(TimeSlotConverter::timeStrToSlotIndex)
                    .toArray();
            result.put(date, slots);
        }
        return result;
    }
}



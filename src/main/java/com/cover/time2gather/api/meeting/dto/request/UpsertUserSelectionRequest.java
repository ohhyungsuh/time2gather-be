package com.cover.time2gather.api.meeting.dto.request;

import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = """
    사용자 시간 선택 요청
    
    각 날짜마다 타입(type)을 명시합니다:
    - "ALL_DAY": 하루 종일 가능 (times 필드 무시)
    - "TIME": 특정 시간 선택 (times 필드 필수)
    """)
public class UpsertUserSelectionRequest {

    @NotNull(message = "선택한 시간대는 필수입니다")
    @Schema(description = "날짜별 선택 정보 배열")
    private List<DateSelection> selections;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "날짜별 선택 정보")
    public static class DateSelection {

        @NotNull(message = "날짜는 필수입니다")
        @Schema(description = "날짜 (YYYY-MM-DD)", example = "2024-12-15")
        private String date;

        @NotNull(message = "타입은 필수입니다")
        @Schema(
            description = "선택 타입",
            allowableValues = {"TIME", "ALL_DAY"},
            example = "TIME"
        )
        private String type;

        @Schema(
            description = "선택한 시간대 (HH:mm 형식). type이 TIME인 경우 필수",
            example = "[\"09:00\", \"10:00\", \"11:00\"]"
        )
        private List<String> times;
    }

    /**
     * API 형식 → 도메인 slotIndex 변환
     */
    public Map<String, int[]> toSlotIndexes() {
        if (selections == null || selections.isEmpty()) {
            throw new IllegalArgumentException("선택 데이터가 없습니다");
        }

        Map<String, int[]> result = new HashMap<>();

        for (DateSelection selection : selections) {
            String date = selection.getDate();
            String type = selection.getType();
            List<String> times = selection.getTimes();

            if ("ALL_DAY".equalsIgnoreCase(type)) {
                // ALL_DAY: 빈 배열
                result.put(date, new int[0]);

            } else if ("TIME".equalsIgnoreCase(type)) {
                // TIME: 시간 배열 필수
                if (times == null || times.isEmpty()) {
                    throw new IllegalArgumentException(
                        String.format("날짜 '%s'는 TIME 타입인데 시간이 지정되지 않았습니다.", date)
                    );
                }

                try {
                    int[] slots = times.stream()
                            .mapToInt(timeStr -> {
                                if (timeStr == null || timeStr.trim().isEmpty()) {
                                    throw new IllegalArgumentException("시간 값이 비어있습니다");
                                }
                                return TimeSlot.fromTimeString(timeStr.trim()).getSlotIndex();
                            })
                            .toArray();
                    result.put(date, slots);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        String.format("날짜 '%s'의 시간 형식이 올바르지 않습니다. %s", date, e.getMessage())
                    );
                }

            } else {
                throw new IllegalArgumentException(
                    String.format("알 수 없는 타입: %s (TIME 또는 ALL_DAY만 가능)", type)
                );
            }
        }

        return result;
    }
}


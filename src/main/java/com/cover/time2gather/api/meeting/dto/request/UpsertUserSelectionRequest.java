package com.cover.time2gather.api.meeting.dto.request;

import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = """
    사용자 시간 선택 요청
    - null = 해당 날짜 선택 안함
    - [] = 하루 종일 가능 (ALL_DAY)
    - ["HH:mm"] = 특정 시간 선택 (TIME)
    """)
public class UpsertUserSelectionRequest {

    @NotNull(message = "선택한 시간대는 필수입니다")
    @Schema(
        description = """
            날짜별 선택 시간대
            
            값의 의미:
            - null = 선택 안함
            - [] (빈 배열) = 하루 종일 (ALL_DAY 전용)
            - ["09:00", ...] = 특정 시간 (TIME 전용)
            """,
        example = "{\"2024-02-15\": [\"09:00\", \"10:00\"], \"2024-02-16\": [], \"2024-02-17\": null}"
    )
    private Map<String, String[]> selections;

    /**
     * API "HH:mm" → 도메인 slotIndex 변환
     *
     * null과 빈 배열의 구분:
     * - null: 맵에 포함하지 않음 (선택 안함)
     * - 빈 배열: 빈 int 배열로 변환 (ALL_DAY - 하루 종일)
     * - 시간 배열: 슬롯 인덱스 배열로 변환 (TIME - 특정 시간)
     */
    public Map<String, int[]> toSlotIndexes() {
        if (selections == null) {
            throw new IllegalArgumentException("선택 데이터가 없습니다");
        }

        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, String[]> entry : selections.entrySet()) {
            String date = entry.getKey();
            String[] times = entry.getValue();

            // null인 경우: 결과 맵에 포함하지 않음 (선택 안함)
            if (times == null) {
                continue;
            }

            // 빈 배열: 하루 종일 선택 (ALL_DAY 타입)
            if (times.length == 0) {
                result.put(date, new int[0]);
                continue;
            }

            // 시간 배열: TIME 타입 처리
            try {
                int[] slots = Arrays.stream(times)
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
        }
        return result;
    }
}


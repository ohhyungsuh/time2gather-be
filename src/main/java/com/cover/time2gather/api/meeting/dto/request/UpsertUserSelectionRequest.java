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
@Schema(description = "사용자 시간 선택 요청")
public class UpsertUserSelectionRequest {

    @NotNull(message = "선택한 시간대는 필수입니다")
    @Schema(description = "날짜별 선택한 시간대 (HH:mm 형식)",
            example = "{\"2024-02-15\": [\"09:00\", \"09:30\", \"10:30\"], \"2024-02-16\": [\"11:00\", \"11:30\"], \"2024-02-17\": []}")
    private Map<String, String[]> selections;

    /**
     * API "HH:mm" → 도메인 slotIndex 변환
     */
    public Map<String, int[]> toSlotIndexes() {
        if (selections == null) {
            throw new IllegalArgumentException("선택 데이터가 없습니다");
        }

        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, String[]> entry : selections.entrySet()) {
            String date = entry.getKey();
            String[] times = entry.getValue();

            // 빈 배열은 그대로 빈 int 배열로 변환
            if (times == null || times.length == 0) {
                result.put(date, new int[0]);
                continue;
            }

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


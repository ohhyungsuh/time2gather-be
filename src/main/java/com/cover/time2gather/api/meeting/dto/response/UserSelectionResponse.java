package com.cover.time2gather.api.meeting.dto.response;

import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
@Schema(description = "User selection response")
public class UserSelectionResponse {

    @Schema(description = "Selected time slots by date (HH:mm format)",
            example = "{\"2024-02-15\": [\"09:00\", \"09:30\", \"10:30\"], \"2024-02-16\": [\"11:00\", \"11:30\"]}")
    private Map<String, String[]> selections;

    /**
     * 도메인 slotIndex → API "HH:mm" 변환
     */
    public static UserSelectionResponse from(Map<String, int[]> slotIndexes) {
        Map<String, String[]> result = new HashMap<>();
        for (Map.Entry<String, int[]> entry : slotIndexes.entrySet()) {
            String date = entry.getKey();
            int[] slots = entry.getValue();
            String[] times = Arrays.stream(slots)
                    .mapToObj(slotIndex -> TimeSlot.fromIndex(slotIndex).toTimeString())
                    .toArray(String[]::new);
            result.put(date, times);
        }
        return new UserSelectionResponse(result);
    }
}


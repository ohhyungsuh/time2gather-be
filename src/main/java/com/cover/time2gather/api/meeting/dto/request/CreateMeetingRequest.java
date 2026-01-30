package com.cover.time2gather.api.meeting.dto.request;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create meeting request")
public class CreateMeetingRequest {

    @NotBlank(message = "{validation.meeting.title.required}")
    @Schema(description = "Meeting title", example = "Project Kickoff Meeting", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Meeting description (optional)", example = "February new project kickoff", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    @Schema(description = "Timezone (optional, default: Asia/Seoul)", example = "Asia/Seoul", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String timezone;

    @Schema(description = "Time slot interval in minutes (optional, default: 60)",
            example = "60",
            allowableValues = {"15", "30", "60"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer intervalMinutes;

    @Schema(description = "Selection type (TIME: hourly, ALL_DAY: full day, default: TIME)",
            example = "TIME",
            allowableValues = {"TIME", "ALL_DAY"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String selectionType;

    @NotNull(message = "{validation.meeting.dates.required}")
    @Size(min = 1, max = 31, message = "{validation.selection.date.min.max}")
    @Schema(description = "Available time slots by date (HH:mm format). Use empty array [] for ALL_DAY type. Max 31 days",
            example = "{\"2024-02-15\": [\"09:00\", \"10:00\", \"11:00\"], \"2024-02-16\": [\"14:00\", \"15:00\"]}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String[]> availableDates;

    @Schema(description = "Enable location voting (default: false)",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean locationVoteEnabled;

    @Size(max = 5, message = "{validation.location.max}")
    @Schema(description = "Location candidates (min 2, max 5). Required when locationVoteEnabled is true",
            example = "[\"Gangnam Station Starbucks\", \"Hongdae A Twosome Place\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<String> locations;

    /**
     * API "HH:mm" → 도메인 slotIndex 변환
     * ALL_DAY 타입인 경우 빈 배열로 변환
     */
    public Map<String, int[]> toSlotIndexes() {
        // ALL_DAY 타입인 경우 빈 배열로 변환
        if ("ALL_DAY".equalsIgnoreCase(selectionType)) {
            Map<String, int[]> result = new HashMap<>();
            for (String date : availableDates.keySet()) {
                result.put(date, new int[0]); // 빈 배열
            }
            return result;
        }

        // TIME 타입 (기존 로직)
        int interval = intervalMinutes != null ? intervalMinutes : TimeSlot.DEFAULT_INTERVAL_MINUTES;
        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, String[]> entry : availableDates.entrySet()) {
            String date = entry.getKey();
            String[] times = entry.getValue();

            // 빈 배열인 경우 (ALL_DAY로 잘못 표시된 경우)
            if (times == null || times.length == 0) {
                result.put(date, new int[0]);
                continue;
            }

            int[] slots = Arrays.stream(times)
                    .mapToInt(timeStr -> TimeSlot.fromTimeString(timeStr, interval).getSlotIndex())
                    .toArray();
            result.put(date, slots);
        }
        return result;
    }

    /**
     * SelectionType enum으로 변환
     */
    public com.cover.time2gather.domain.meeting.SelectionType getSelectionTypeEnum() {
        if ("ALL_DAY".equalsIgnoreCase(selectionType)) {
            return com.cover.time2gather.domain.meeting.SelectionType.ALL_DAY;
        }
        return com.cover.time2gather.domain.meeting.SelectionType.TIME;
    }

    /**
     * 장소 투표 활성화 여부 반환 (null인 경우 false)
     */
    public boolean isLocationVoteEnabled() {
        return Boolean.TRUE.equals(locationVoteEnabled);
    }

    /**
     * 장소 목록 유효성 검증
     * locationVoteEnabled가 true인 경우 최소 2개 필요
     */
    public void validateLocations() {
        if (isLocationVoteEnabled()) {
            if (locations == null || locations.size() < 2) {
                throw new BusinessException(ErrorCode.LOCATION_MIN_FOR_VOTE);
            }
            if (locations.size() > 5) {
                throw new BusinessException(ErrorCode.LOCATION_MAX_EXCEEDED);
            }
            // 빈 문자열 또는 공백만 있는 장소 이름 검증
            for (String location : locations) {
                if (location == null || location.trim().isEmpty()) {
                    throw new BusinessException(ErrorCode.LOCATION_NAME_REQUIRED);
                }
            }
        }
    }
}



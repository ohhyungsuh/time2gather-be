package com.cover.time2gather.api.meeting.dto.request;

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
@Schema(description = "모임 생성 요청")
public class CreateMeetingRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Schema(description = "모임 제목", example = "프로젝트 킥오프 미팅", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "모임 설명 (선택사항)", example = "2월 신규 프로젝트 시작 회의", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    @Schema(description = "타임존 (선택사항, 기본값: Asia/Seoul)", example = "Asia/Seoul", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String timezone;

    @Schema(description = "시간 슬롯 간격 (분, 선택사항, 기본값: 60분)",
            example = "60",
            allowableValues = {"15", "30", "60"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer intervalMinutes;

    @Schema(description = "선택 타입 (TIME: 시간 단위, ALL_DAY: 일 단위, 기본값: TIME)",
            example = "TIME",
            allowableValues = {"TIME", "ALL_DAY"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String selectionType;

    @NotNull(message = "가능한 날짜/시간은 필수입니다")
    @Schema(description = "날짜별 가능한 시간대 (HH:mm 형식). ALL_DAY 타입인 경우 빈 배열 [] 사용",
            example = "{\"2024-02-15\": [\"09:00\", \"10:00\", \"11:00\"], \"2024-02-16\": [\"14:00\", \"15:00\"]}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String[]> availableDates;

    @Schema(description = "장소 투표 활성화 여부 (기본값: false)",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean locationVoteEnabled;

    @Size(max = 5, message = "장소는 최대 5개까지 추가할 수 있습니다")
    @Schema(description = "장소 후보 목록 (최소 2개, 최대 5개). locationVoteEnabled가 true인 경우 필수",
            example = "[\"강남역 스타벅스\", \"홍대입구역 투썸플레이스\"]",
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
                throw new IllegalArgumentException("장소 투표를 활성화하려면 최소 2개의 장소가 필요합니다.");
            }
            if (locations.size() > 5) {
                throw new IllegalArgumentException("장소는 최대 5개까지 추가할 수 있습니다.");
            }
            // 빈 문자열 또는 공백만 있는 장소 이름 검증
            for (String location : locations) {
                if (location == null || location.trim().isEmpty()) {
                    throw new IllegalArgumentException("장소 이름은 비어있을 수 없습니다.");
                }
            }
        }
    }
}



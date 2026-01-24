package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 도메인 레이어의 모임 상세 데이터
 * Service 레이어에서 반환하는 순수 도메인 모델
 */
@Getter
@AllArgsConstructor
public class MeetingDetailData {
    private final Meeting meeting;
    private final User host;
    private final List<User> participants;
    private final List<MeetingUserSelection> selections;
    private final ScheduleData schedule;
    private final SummaryData summary;
    private final boolean isParticipated; // 현재 사용자의 참여 여부
    private final LocationData locationData; // 장소 투표 정보

    // 기존 생성자 호환용 (장소 데이터 없음)
    public MeetingDetailData(
            Meeting meeting, User host, List<User> participants,
            List<MeetingUserSelection> selections, ScheduleData schedule,
            SummaryData summary, boolean isParticipated
    ) {
        this(meeting, host, participants, selections, schedule, summary, isParticipated, null);
    }

    /**
     * 날짜/시간별 참여자 정보
     * Map<날짜, Map<시간슬롯, 참여자목록>>
     */
    @Getter
    @AllArgsConstructor
    public static class ScheduleData {
        private final Map<String, Map<Integer, List<User>>> dateTimeUserMap;
    }

    /**
     * 요약 정보
     */
    @Getter
    @AllArgsConstructor
    public static class SummaryData {
        private final int totalParticipants;
        private final List<BestSlot> bestSlots;
    }

    /**
     * 베스트 슬롯 정보 (연속 시간 병합 지원)
     * 
     * 연속된 시간 슬롯은 하나로 병합되어 startSlotIndex ~ endSlotIndex 범위로 표현됨
     * 예: 14:00, 15:00, 16:00 연속 → startSlotIndex=14, endSlotIndex=16
     * 
     * 단일 슬롯인 경우 startSlotIndex == endSlotIndex
     */
    @Getter
    public static class BestSlot {
        private final String date;
        private final int startSlotIndex;
        private final int endSlotIndex;
        private final int count;
        private final String percentage;
        private final List<User> participants;

        /**
         * 연속 슬롯 병합용 생성자
         */
        public BestSlot(String date, int startSlotIndex, int endSlotIndex, 
                        int count, double percentageValue, List<User> participants) {
            this.date = date;
            this.startSlotIndex = startSlotIndex;
            this.endSlotIndex = endSlotIndex;
            this.count = count;
            this.percentage = Math.round(percentageValue) + "%";
            this.participants = participants != null ? participants : List.of();
        }

        /**
         * 단일 슬롯용 생성자 (기존 호환)
         */
        public BestSlot(String date, int slotIndex, int count, double percentageValue) {
            this(date, slotIndex, slotIndex, count, percentageValue, List.of());
        }

        /**
         * 기존 호환용: slotIndex 반환 (단일 슬롯인 경우 startSlotIndex 반환)
         * @deprecated startSlotIndex, endSlotIndex 사용 권장
         */
        @Deprecated
        public int getSlotIndex() {
            return startSlotIndex;
        }

        /**
         * 연속 슬롯 범위인지 확인
         */
        public boolean isRange() {
            return startSlotIndex != endSlotIndex;
        }
    }

    /**
     * 장소 투표 정보
     */
    @Getter
    @AllArgsConstructor
    public static class LocationData {
        private final boolean enabled; // 장소 투표 활성화 여부
        private final List<LocationInfo> locations; // 장소 목록 + 투표 수
        private final LocationInfo confirmedLocation; // 확정된 장소 (없으면 null)
    }

    /**
     * 장소 정보
     */
    @Getter
    @AllArgsConstructor
    public static class LocationInfo {
        private final Long id;
        private final String name;
        private final int displayOrder;
        private final int voteCount; // 투표 수
        private final String percentage; // 투표 비율
        private final List<User> voters; // 투표한 사용자 목록
    }
}

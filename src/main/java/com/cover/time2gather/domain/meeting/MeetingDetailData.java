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
     * 베스트 슬롯 정보
     */
    @Getter
    @AllArgsConstructor
    public static class BestSlot {
        private final String date;
        private final int slotIndex;
        private final int count;
        private final String percentage;

        public BestSlot(String date, int slotIndex, int count, double percentageValue) {
            this.date = date;
            this.slotIndex = slotIndex;
            this.count = count;
            // 소수점 제거하고 %로 표시
            this.percentage = Math.round(percentageValue) + "%";
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

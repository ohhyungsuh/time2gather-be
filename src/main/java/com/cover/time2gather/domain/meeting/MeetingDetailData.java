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
        private final double percentage;
    }
}

package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@Schema(description = "모임 상세 응답")
public class MeetingDetailResponse {

    @Schema(description = "모임 정보")
    private MeetingInfo meeting;

    @Schema(description = "참여자 목록")
    private List<ParticipantInfo> participants;

    @Schema(description = "날짜/시간별 참여 가능한 사용자 목록")
    private Map<String, Map<String, List<ParticipantInfo>>> schedule;

    @Schema(description = "요약 정보")
    private SummaryInfo summary;

    @Getter
    @AllArgsConstructor
    @Schema(description = "모임 기본 정보")
    public static class MeetingInfo {
        @Schema(description = "모임 ID", example = "1")
        private Long id;

        @Schema(description = "모임 코드", example = "mtg_a3f8k2md9x")
        private String code;

        @Schema(description = "모임 제목", example = "프로젝트 킥오프 미팅")
        private String title;

        @Schema(description = "모임 설명", example = "2월 신규 프로젝트 시작 회의")
        private String description;

        @Schema(description = "방장 정보")
        private HostInfo host;

        @Schema(description = "타임존", example = "Asia/Seoul")
        private String timezone;

        @Schema(description = "가능한 날짜/시간대",
                example = "{\"2024-02-15\": [\"09:00\", \"09:30\", \"10:00\"], \"2024-02-16\": [\"11:00\", \"11:30\"]}")
        private Map<String, String[]> availableDates;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "방장 정보")
    public static class HostInfo {
        @Schema(description = "사용자 ID", example = "1")
        private Long id;

        @Schema(description = "사용자명", example = "jinwoo")
        private String username;

        @Schema(description = "프로필 이미지 URL", example = "https://...")
        private String profileImageUrl;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "참여자 정보")
    public static class ParticipantInfo {
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "사용자명", example = "jinwoo")
        private String username;

        @Schema(description = "프로필 이미지 URL", example = "https://...")
        private String profileImageUrl;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "요약 정보")
    public static class SummaryInfo {
        @Schema(description = "총 참여자 수", example = "5")
        private int totalParticipants;

        @Schema(description = "베스트 시간대 (가장 많은 사람이 가능한 시간대)")
        private List<BestSlot> bestSlots;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "베스트 시간대")
    public static class BestSlot {
        @Schema(description = "날짜", example = "2024-02-15")
        private String date;

        @Schema(description = "시간", example = "09:00")
        private String time;

        @Schema(description = "가능한 인원 수", example = "4")
        private int count;

        @Schema(description = "가능 비율 (%)", example = "80.0")
        private double percentage;
    }
}

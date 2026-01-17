package com.cover.time2gather.domain.chat;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring AI Tool Calling을 위한 미팅 조회 도구 모음
 */
@Component
@RequiredArgsConstructor
public class MeetingQueryTools {

    private final MeetingQueryService meetingQueryService;
    private final MeetingUserSelectionRepository meetingUserSelectionRepository;

    @Tool(description = "사용자의 모든 미팅 목록을 조회합니다. 호스트로 만든 미팅과 참여한 미팅 모두 포함됩니다.")
    public String getAllMeetings(@ToolParam(description = "사용자 ID") Long userId) {
        List<Meeting> meetings = meetingQueryService.findAllMeetingsByUser(userId);

        if (meetings.isEmpty()) {
            return "등록된 미팅이 없습니다.";
        }

        return formatMeetingList(meetings);
    }

    @Tool(description = "제목으로 미팅을 검색합니다. 키워드가 포함된 미팅을 찾습니다.")
    public String searchMeetingsByTitle(
            @ToolParam(description = "사용자 ID") Long userId,
            @ToolParam(description = "검색할 키워드") String keyword) {
        List<Meeting> meetings = meetingQueryService.findMeetingsByTitle(userId, keyword);

        if (meetings.isEmpty()) {
            return "'" + keyword + "'에 대한 검색 결과가 없습니다.";
        }

        return formatMeetingList(meetings);
    }

    @Tool(description = "사용자의 미팅 개수를 조회합니다.")
    public String getMeetingCount(@ToolParam(description = "사용자 ID") Long userId) {
        List<Meeting> meetings = meetingQueryService.findAllMeetingsByUser(userId);
        return "총 " + meetings.size() + "개의 미팅이 있습니다.";
    }

    @Tool(description = "다가오는 미팅을 조회합니다. 오늘 이후 날짜가 포함된 미팅만 반환합니다.")
    public String getUpcomingMeetings(@ToolParam(description = "사용자 ID") Long userId) {
        List<Meeting> meetings = meetingQueryService.findUpcomingMeetings(userId);

        if (meetings.isEmpty()) {
            return "다가오는 미팅이 없습니다.";
        }

        return "📅 다가오는 미팅 (" + meetings.size() + "개)\n\n" + formatMeetingList(meetings);
    }

    @Tool(description = "지난 미팅을 조회합니다. 모든 날짜가 오늘 이전인 미팅만 반환합니다.")
    public String getPastMeetings(@ToolParam(description = "사용자 ID") Long userId) {
        List<Meeting> meetings = meetingQueryService.findPastMeetings(userId);

        if (meetings.isEmpty()) {
            return "지난 미팅이 없습니다.";
        }

        return "📋 지난 미팅 (" + meetings.size() + "개)\n\n" + formatMeetingList(meetings);
    }

    @Tool(description = "미팅 코드로 미팅 상세 정보를 조회합니다. 날짜, 시간대, 확정 여부 등을 확인할 수 있습니다.")
    public String getMeetingDetail(
            @ToolParam(description = "사용자 ID") Long userId,
            @ToolParam(description = "미팅 코드") String meetingCode) {
        Meeting meeting = meetingQueryService.findMeetingByCode(userId, meetingCode);

        if (meeting == null) {
            return "미팅을 찾을 수 없습니다: " + meetingCode;
        }

        return formatMeetingDetail(meeting);
    }

    @Tool(description = "미팅의 참석자 수를 조회합니다.")
    public String getMeetingParticipants(
            @ToolParam(description = "사용자 ID") Long userId,
            @ToolParam(description = "미팅 코드") String meetingCode) {
        Meeting meeting = meetingQueryService.findMeetingByCode(userId, meetingCode);

        if (meeting == null) {
            return "미팅을 찾을 수 없습니다: " + meetingCode;
        }

        List<MeetingUserSelection> selections = meetingUserSelectionRepository.findAllByMeetingId(meeting.getId());
        int participantCount = selections.size();

        return "👥 '" + meeting.getTitle() + "' 참석자: " + participantCount + "명";
    }

    @Tool(description = "사용자의 미팅 통계를 조회합니다. 전체 미팅 수, 다가오는 미팅 수, 지난 미팅 수를 확인할 수 있습니다.")
    public String getMeetingStats(@ToolParam(description = "사용자 ID") Long userId) {
        List<Meeting> allMeetings = meetingQueryService.findAllMeetingsByUser(userId);
        List<Meeting> upcomingMeetings = meetingQueryService.findUpcomingMeetings(userId);
        List<Meeting> pastMeetings = meetingQueryService.findPastMeetings(userId);

        long confirmedCount = allMeetings.stream().filter(Meeting::isConfirmed).count();

        StringBuilder sb = new StringBuilder();
        sb.append("📊 미팅 통계\n\n");
        sb.append("• 전체 미팅: ").append(allMeetings.size()).append("개\n");
        sb.append("• 다가오는 미팅: ").append(upcomingMeetings.size()).append("개\n");
        sb.append("• 지난 미팅: ").append(pastMeetings.size()).append("개\n");
        sb.append("• 확정된 미팅: ").append(confirmedCount).append("개");

        return sb.toString();
    }

    private String formatMeetingList(List<Meeting> meetings) {
        return meetings.stream()
                .map(this::formatMeeting)
                .collect(Collectors.joining("\n\n"));
    }

    private String formatMeeting(Meeting meeting) {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(meeting.getTitle());

        if (meeting.getDescription() != null && !meeting.getDescription().isBlank()) {
            sb.append("\n  설명: ").append(meeting.getDescription());
        }

        if (meeting.getAvailableDates() != null && !meeting.getAvailableDates().isEmpty()) {
            String dates = meeting.getAvailableDates().keySet().stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
            sb.append("\n  날짜: ").append(dates);
        }

        sb.append("\n  유형: ").append(meeting.getSelectionType().name());

        if (meeting.isConfirmed()) {
            sb.append("\n  ✅ 확정됨: ").append(meeting.getConfirmedDate());
            if (meeting.getConfirmedSlotIndex() != null) {
                TimeSlot timeSlot = TimeSlot.fromIndex(meeting.getConfirmedSlotIndex(), meeting.getIntervalMinutes());
                sb.append(" ").append(timeSlot.toTimeString());
            }
        }

        return sb.toString();
    }

    private String formatMeetingDetail(Meeting meeting) {
        StringBuilder sb = new StringBuilder();
        sb.append("📌 ").append(meeting.getTitle()).append("\n\n");

        if (meeting.getDescription() != null && !meeting.getDescription().isBlank()) {
            sb.append("설명: ").append(meeting.getDescription()).append("\n");
        }

        sb.append("미팅 코드: ").append(meeting.getMeetingCode()).append("\n");
        sb.append("유형: ").append(meeting.getSelectionType().name()).append("\n");

        if (meeting.getAvailableDates() != null && !meeting.getAvailableDates().isEmpty()) {
            sb.append("\n📅 가능한 날짜:\n");
            meeting.getAvailableDates().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        sb.append("  • ").append(entry.getKey());
                        if (entry.getValue() != null && entry.getValue().length > 0) {
                            sb.append(" (").append(entry.getValue().length).append("개 시간대)");
                        } else {
                            sb.append(" (종일)");
                        }
                        sb.append("\n");
                    });
        }

        if (meeting.isConfirmed()) {
            sb.append("\n✅ 확정된 일정: ").append(meeting.getConfirmedDate());
            if (meeting.getConfirmedSlotIndex() != null) {
                TimeSlot timeSlot = TimeSlot.fromIndex(meeting.getConfirmedSlotIndex(), meeting.getIntervalMinutes());
                sb.append(" ").append(timeSlot.toTimeString());
            }
            sb.append("\n확정 시각: ").append(meeting.getConfirmedAt());
        } else {
            sb.append("\n⏳ 아직 확정되지 않음");
        }

        return sb.toString();
    }
}

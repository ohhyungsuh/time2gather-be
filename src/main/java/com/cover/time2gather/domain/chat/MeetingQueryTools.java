package com.cover.time2gather.domain.chat;

import com.cover.time2gather.domain.meeting.Meeting;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI Tool Calling을 위한 미팅 조회 도구 모음
 */
@Component
@RequiredArgsConstructor
public class MeetingQueryTools {

    private final MeetingQueryService meetingQueryService;

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

        return sb.toString();
    }
}

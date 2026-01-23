// package com.cover.time2gather.domain.chat;
//
// import com.cover.time2gather.domain.meeting.Meeting;
// import com.cover.time2gather.domain.meeting.vo.TimeSlot;
// import lombok.RequiredArgsConstructor;
// import org.springframework.ai.tool.annotation.Tool;
// import org.springframework.ai.tool.annotation.ToolParam;
// import org.springframework.stereotype.Component;
//
// import java.time.LocalDate;
// import java.util.List;
// import java.util.stream.Collectors;
//
// /**
//  * Spring AI Tool Calling을 위한 미팅 조회 도구 모음
//  * 확정된 일정 기반으로만 정보를 제공합니다.
//  */
// @Component
// @RequiredArgsConstructor
// public class MeetingQueryTools {
//
//     private final MeetingQueryService meetingQueryService;
//
//     @Tool(description = "사용자의 확정된 미팅 목록을 조회합니다. 일정이 확정된 미팅만 반환됩니다.")
//     public String getConfirmedMeetings(@ToolParam(description = "사용자 ID") Long userId) {
//         List<Meeting> meetings = meetingQueryService.findAllMeetingsByUser(userId).stream()
//                 .filter(Meeting::isConfirmed)
//                 .toList();
//
//         if (meetings.isEmpty()) {
//             return "확정된 미팅이 없습니다.";
//         }
//
//         return "✅ 확정된 미팅 (" + meetings.size() + "개)\n\n" + formatConfirmedMeetingList(meetings);
//     }
//
//     @Tool(description = "제목으로 확정된 미팅을 검색합니다. 키워드가 포함된 확정된 미팅을 찾습니다.")
//     public String searchConfirmedMeetingsByTitle(
//             @ToolParam(description = "사용자 ID") Long userId,
//             @ToolParam(description = "검색할 키워드") String keyword) {
//         List<Meeting> meetings = meetingQueryService.findMeetingsByTitle(userId, keyword).stream()
//                 .filter(Meeting::isConfirmed)
//                 .toList();
//
//         if (meetings.isEmpty()) {
//             return "'" + keyword + "'에 해당하는 확정된 미팅이 없습니다.";
//         }
//
//         return formatConfirmedMeetingList(meetings);
//     }
//
//     @Tool(description = "다가오는 확정된 미팅을 조회합니다. 오늘 이후에 확정된 미팅만 반환합니다.")
//     public String getUpcomingConfirmedMeetings(@ToolParam(description = "사용자 ID") Long userId) {
//         LocalDate today = LocalDate.now();
//         List<Meeting> meetings = meetingQueryService.findAllMeetingsByUser(userId).stream()
//                 .filter(Meeting::isConfirmed)
//                 .filter(m -> m.getConfirmedDate() != null && !m.getConfirmedDate().isBefore(today))
//                 .sorted((a, b) -> a.getConfirmedDate().compareTo(b.getConfirmedDate()))
//                 .toList();
//
//         if (meetings.isEmpty()) {
//             return "다가오는 확정된 미팅이 없습니다.";
//         }
//
//         return "📅 다가오는 확정된 미팅 (" + meetings.size() + "개)\n\n" + formatConfirmedMeetingList(meetings);
//     }
//
//     @Tool(description = "미팅 코드로 확정된 미팅 상세 정보를 조회합니다. 확정된 날짜와 시간을 확인할 수 있습니다.")
//     public String getConfirmedMeetingDetail(
//             @ToolParam(description = "사용자 ID") Long userId,
//             @ToolParam(description = "미팅 코드") String meetingCode) {
//         Meeting meeting = meetingQueryService.findMeetingByCode(userId, meetingCode);
//
//         if (meeting == null) {
//             return "미팅을 찾을 수 없습니다: " + meetingCode;
//         }
//
//         if (!meeting.isConfirmed()) {
//             return "'" + meeting.getTitle() + "' 미팅은 아직 일정이 확정되지 않았습니다.";
//         }
//
//         return formatConfirmedMeetingDetail(meeting);
//     }
//
//     @Tool(description = "확정된 미팅 통계를 조회합니다. 확정된 미팅 수와 다가오는 확정 일정 수를 확인할 수 있습니다.")
//     public String getConfirmedMeetingStats(@ToolParam(description = "사용자 ID") Long userId) {
//         LocalDate today = LocalDate.now();
//         List<Meeting> confirmedMeetings = meetingQueryService.findAllMeetingsByUser(userId).stream()
//                 .filter(Meeting::isConfirmed)
//                 .toList();
//
//         long upcomingCount = confirmedMeetings.stream()
//                 .filter(m -> m.getConfirmedDate() != null && !m.getConfirmedDate().isBefore(today))
//                 .count();
//
//         long pastCount = confirmedMeetings.stream()
//                 .filter(m -> m.getConfirmedDate() != null && m.getConfirmedDate().isBefore(today))
//                 .count();
//
//         StringBuilder sb = new StringBuilder();
//         sb.append("📊 확정된 미팅 통계\n\n");
//         sb.append("• 전체 확정된 미팅: ").append(confirmedMeetings.size()).append("개\n");
//         sb.append("• 다가오는 일정: ").append(upcomingCount).append("개\n");
//         sb.append("• 지난 일정: ").append(pastCount).append("개");
//
//         return sb.toString();
//     }
//
//     private String formatConfirmedMeetingList(List<Meeting> meetings) {
//         return meetings.stream()
//                 .map(this::formatConfirmedMeeting)
//                 .collect(Collectors.joining("\n\n"));
//     }
//
//     private String formatConfirmedMeeting(Meeting meeting) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("- ").append(meeting.getTitle());
//
//         sb.append("\n  📅 확정 일정: ").append(meeting.getConfirmedDate());
//         if (meeting.getConfirmedSlotIndex() != null) {
//             TimeSlot timeSlot = TimeSlot.fromIndex(meeting.getConfirmedSlotIndex(), meeting.getIntervalMinutes());
//             sb.append(" ").append(timeSlot.toTimeString());
//         }
//
//         if (meeting.getDescription() != null && !meeting.getDescription().isBlank()) {
//             sb.append("\n  설명: ").append(meeting.getDescription());
//         }
//
//         return sb.toString();
//     }
//
//     private String formatConfirmedMeetingDetail(Meeting meeting) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("📌 ").append(meeting.getTitle()).append("\n\n");
//
//         if (meeting.getDescription() != null && !meeting.getDescription().isBlank()) {
//             sb.append("설명: ").append(meeting.getDescription()).append("\n");
//         }
//
//         sb.append("미팅 코드: ").append(meeting.getMeetingCode()).append("\n");
//
//         sb.append("\n✅ 확정된 일정: ").append(meeting.getConfirmedDate());
//         if (meeting.getConfirmedSlotIndex() != null) {
//             TimeSlot timeSlot = TimeSlot.fromIndex(meeting.getConfirmedSlotIndex(), meeting.getIntervalMinutes());
//             sb.append(" ").append(timeSlot.toTimeString());
//         }
//         sb.append("\n확정 시각: ").append(meeting.getConfirmedAt());
//
//         return sb.toString();
//     }
// }

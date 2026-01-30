package com.cover.time2gather.api.mcp;

import com.cover.time2gather.config.security.AuthenticatedUserService;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.meeting.service.CalendarExportService;
import com.cover.time2gather.domain.meeting.service.MeetingFacadeService;
import com.cover.time2gather.domain.meeting.service.MeetingLocationService;
import com.cover.time2gather.domain.meeting.service.MeetingService;
import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.util.MessageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Time2Gather MCP Server Tools
 * PlayMCP 연동을 위한 MCP 도구 정의
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpMeetingTools {

    private final MeetingService meetingService;
    private final MeetingFacadeService meetingFacadeService;
    private final MeetingLocationService meetingLocationService;
    private final CalendarExportService calendarExportService;
    private final MeetingRepository meetingRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @McpTool(name = "get_meeting", description = "모임 상세 정보를 조회합니다. 모임 코드로 해당 모임의 제목, 설명, 날짜 옵션, 참여자 투표 현황, 확정 여부 등을 확인할 수 있습니다.")
    public MeetingDetailResponse getMeeting(
            @McpToolParam(description = "조회할 모임의 코드 (예: mtg_abc123)") String meetingCode
    ) {
        log.info("MCP getMeeting called with meetingCode: {}", meetingCode);
        
        MeetingDetailData detailData = meetingService.getMeetingDetailData(meetingCode, null);
        return MeetingDetailResponse.from(detailData);
    }

    @McpTool(name = "create_meeting", description = "새로운 모임을 생성합니다. 모임 제목, 선택 가능한 날짜들, 타임존, 선택 타입(시간 단위/종일)을 지정할 수 있습니다. 생성된 모임 코드를 반환합니다. 인증된 사용자가 호스트가 됩니다.")
    public CreateMeetingResponse createMeeting(
            @McpToolParam(description = "모임 제목") String title,
            @McpToolParam(description = "모임 설명 (선택사항)") String description,
            @McpToolParam(description = "선택 가능한 날짜 목록 (yyyy-MM-dd 형식, 쉼표로 구분)") String dates,
            @McpToolParam(description = "타임존 (기본값: Asia/Seoul)") String timezone,
            @McpToolParam(description = "선택 타입: TIME(시간 단위) 또는 ALL_DAY(종일). 기본값: ALL_DAY") String selectionType,
            @McpToolParam(description = "TIME 타입인 경우 시간 슬롯들 (예: 09:00,10:00,11:00). ALL_DAY인 경우 무시됩니다.") String timeSlots,
            @McpToolParam(description = "시간 간격(분). 기본값: 60") Integer intervalMinutes
    ) {
        Long hostUserId = authenticatedUserService.getRequiredCurrentUserId();
        log.info("MCP createMeeting called by userId: {}, title: {}, dates: {}", hostUserId, title, dates);
        
        // 파라미터 기본값 처리
        String tz = timezone != null ? timezone : "Asia/Seoul";
        SelectionType type = parseSelectionType(selectionType);
        int interval = intervalMinutes != null ? intervalMinutes : TimeSlot.DEFAULT_INTERVAL_MINUTES;
        
        // 날짜 파싱
        String[] dateArray = dates.split(",");
        Map<String, int[]> availableDates = new HashMap<>();
        
        if (type == SelectionType.ALL_DAY) {
            // ALL_DAY: 각 날짜에 빈 배열
            for (String date : dateArray) {
                availableDates.put(date.trim(), new int[0]);
            }
        } else {
            // TIME: 각 날짜에 시간 슬롯 인덱스 배열
            int[] slotIndexes = parseTimeSlots(timeSlots, interval);
            for (String date : dateArray) {
                availableDates.put(date.trim(), slotIndexes);
            }
        }
        
        Meeting meeting = meetingService.createMeeting(
                hostUserId,
                title,
                description,
                tz,
                type,
                interval,
                availableDates
        );
        
        return new CreateMeetingResponse(
                meeting.getMeetingCode(),
                meeting.getTitle(),
                MessageProvider.getMessage("mcp.meeting.created")
        );
    }

    @McpTool(name = "vote_time", description = "모임에 시간 투표를 합니다. 인증된 사용자가 가능한 날짜와 시간대를 선택합니다.")
    public VoteResponse voteTime(
            @McpToolParam(description = "모임 코드") String meetingCode,
            @McpToolParam(description = "선택한 날짜와 시간 (형식: 날짜1:시간1,시간2;날짜2:시간1,시간2 예: 2024-02-15:09:00,10:00;2024-02-16:14:00,15:00). ALL_DAY 타입이면 시간 부분 생략 (예: 2024-02-15;2024-02-16)") String selections
    ) {
        Long userId = authenticatedUserService.getRequiredCurrentUserId();
        log.info("MCP voteTime called by userId: {}, meetingCode: {}", userId, meetingCode);
        
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);
        Map<String, int[]> slotIndexes = parseSelections(selections, meeting.getSelectionType(), meeting.getIntervalMinutes());
        
        meetingFacadeService.upsertUserSelections(meetingCode, userId, slotIndexes);
        
        return new VoteResponse(true, MessageProvider.getMessage("mcp.vote.time.saved"));
    }

    @McpTool(name = "vote_location", description = "모임 장소에 투표합니다. 장소 투표가 활성화된 모임에서만 사용 가능합니다. 인증된 사용자가 투표합니다.")
    public VoteResponse voteLocation(
            @McpToolParam(description = "모임 코드") String meetingCode,
            @McpToolParam(description = "선택한 장소 ID 목록 (쉼표로 구분)") String locationIds
    ) {
        Long userId = authenticatedUserService.getRequiredCurrentUserId();
        log.info("MCP voteLocation called by userId: {}, meetingCode: {}", userId, meetingCode);
        
        List<Long> ids = parseLocationIds(locationIds);
        meetingLocationService.voteLocations(meetingCode, userId, ids);
        
        return new VoteResponse(true, MessageProvider.getMessage("mcp.vote.location.saved"));
    }

    @McpTool(name = "confirm_meeting", description = "모임 일정을 확정합니다. 호스트만 사용할 수 있습니다.")
    public ConfirmResponse confirmMeeting(
            @McpToolParam(description = "모임 코드") String meetingCode,
            @McpToolParam(description = "확정할 날짜 (yyyy-MM-dd 형식)") String date,
            @McpToolParam(description = "확정할 시간 슬롯 인덱스 (TIME 타입인 경우). ALL_DAY 타입이면 null 허용") Integer slotIndex
    ) {
        log.info("MCP confirmMeeting called with meetingCode: {}, date: {}", meetingCode, date);
        
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);
        LocalDate confirmDate = LocalDate.parse(date);
        
        meetingService.confirmMeeting(meeting, confirmDate, slotIndex);
        
        return new ConfirmResponse(true, MessageProvider.getMessage("mcp.meeting.confirmed"), date, slotIndex);
    }

    @McpTool(name = "confirm_location", description = "모임 장소를 확정합니다. 호스트만 사용할 수 있으며, 장소 투표가 활성화된 모임에서만 사용 가능합니다. 인증된 사용자가 호스트여야 합니다.")
    public ConfirmResponse confirmLocation(
            @McpToolParam(description = "모임 코드") String meetingCode,
            @McpToolParam(description = "확정할 장소 ID") Long locationId
    ) {
        Long hostUserId = authenticatedUserService.getRequiredCurrentUserId();
        log.info("MCP confirmLocation called by userId: {}, meetingCode: {}, locationId: {}", hostUserId, meetingCode, locationId);
        
        meetingLocationService.confirmLocation(meetingCode, hostUserId, locationId);
        
        return new ConfirmResponse(true, MessageProvider.getMessage("mcp.location.confirmed"), null, null);
    }

    @McpTool(name = "export_calendar", description = "확정된 모임 일정을 ICS 캘린더 파일로 내보냅니다. Google Calendar, Apple Calendar 등에서 가져오기 할 수 있는 형식입니다.")
    public ExportCalendarResponse exportCalendar(
            @McpToolParam(description = "모임 코드") String meetingCode,
            @McpToolParam(description = "내보낼 날짜 (yyyy-MM-dd 형식)") String date,
            @McpToolParam(description = "내보낼 시간 (HH:mm 형식). ALL_DAY 타입이면 'ALL_DAY' 입력") String timeSlot
    ) {
        log.info("MCP exportCalendar called with meetingCode: {}", meetingCode);
        
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);
        
        byte[] icsData = calendarExportService.createIcsFile(
                meeting.getTitle(),
                meeting.getDescription(),
                date,
                timeSlot,
                meeting.getTimezone(),
                meeting.getIntervalMinutes()
        );
        
        // Base64로 인코딩하여 반환
        String icsBase64 = Base64.getEncoder().encodeToString(icsData);
        
        return new ExportCalendarResponse(
                true,
                MessageProvider.getMessage("mcp.ics.created"),
                icsBase64,
                meeting.getTitle() + ".ics"
        );
    }

    // ==================== Helper Methods ====================

    private SelectionType parseSelectionType(String type) {
        if (type == null || type.isEmpty()) {
            return SelectionType.ALL_DAY;
        }
        try {
            return SelectionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SelectionType.ALL_DAY;
        }
    }

    private int[] parseTimeSlots(String timeSlots, int intervalMinutes) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            // 기본값: 9시~18시
            return new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17};
        }
        
        String[] times = timeSlots.split(",");
        int[] indexes = new int[times.length];
        
        for (int i = 0; i < times.length; i++) {
            String time = times[i].trim();
            TimeSlot slot = TimeSlot.fromTimeString(time, intervalMinutes);
            indexes[i] = slot.getSlotIndex();
        }
        
        return indexes;
    }

    private Map<String, int[]> parseSelections(String selections, SelectionType type, int intervalMinutes) {
        Map<String, int[]> result = new HashMap<>();
        
        if (selections == null || selections.isEmpty()) {
            return result;
        }
        
        // 형식: "2024-02-15:09:00,10:00;2024-02-16:14:00,15:00" (TIME)
        // 또는: "2024-02-15;2024-02-16" (ALL_DAY)
        String[] dateSelections = selections.split(";");
        
        for (String dateSelection : dateSelections) {
            dateSelection = dateSelection.trim();
            if (dateSelection.isEmpty()) continue;
            
            if (type == SelectionType.ALL_DAY) {
                // ALL_DAY: 날짜만 있음
                result.put(dateSelection, new int[0]);
            } else {
                // TIME: 날짜:시간1,시간2 형식
                String[] parts = dateSelection.split(":");
                if (parts.length >= 2) {
                    String date = parts[0];
                    // 시간 부분은 나머지 모두 합침 (HH:mm 형식 때문에 : 포함)
                    String timePart = dateSelection.substring(date.length() + 1);
                    String[] times = timePart.split(",");
                    
                    int[] indexes = new int[times.length];
                    for (int i = 0; i < times.length; i++) {
                        TimeSlot slot = TimeSlot.fromTimeString(times[i].trim(), intervalMinutes);
                        indexes[i] = slot.getSlotIndex();
                    }
                    result.put(date, indexes);
                }
            }
        }
        
        return result;
    }

    private List<Long> parseLocationIds(String locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            return List.of();
        }
        
        String[] ids = locationIds.split(",");
        return java.util.Arrays.stream(ids)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    // ==================== Response DTOs ====================

    public record MeetingDetailResponse(
            String meetingCode,
            String title,
            String description,
            String timezone,
            String selectionType,
            Integer intervalMinutes,
            boolean isConfirmed,
            String confirmedDate,
            Integer confirmedSlotIndex,
            String hostName,
            int participantCount,
            List<BestSlotInfo> bestSlots,
            boolean locationVoteEnabled
    ) {
        public static MeetingDetailResponse from(MeetingDetailData data) {
            Meeting meeting = data.getMeeting();
            MeetingDetailData.SummaryData summary = data.getSummary();
            
            List<BestSlotInfo> bestSlots = summary.getBestSlots().stream()
                    .map(slot -> new BestSlotInfo(
                            slot.getDate(),
                            slot.getSlotIndex(),
                            slot.getCount(),
                            slot.getPercentage()
                    ))
                    .collect(Collectors.toList());
            
            return new MeetingDetailResponse(
                    meeting.getMeetingCode(),
                    meeting.getTitle(),
                    meeting.getDescription(),
                    meeting.getTimezone(),
                    meeting.getSelectionType().name(),
                    meeting.getIntervalMinutes(),
                    meeting.isConfirmed(),
                    meeting.getConfirmedDate() != null ? meeting.getConfirmedDate().toString() : null,
                    meeting.getConfirmedSlotIndex(),
                    data.getHost().getUsername(),
                    summary.getTotalParticipants(),
                    bestSlots,
                    meeting.isLocationVoteEnabled()
            );
        }
    }

    public record BestSlotInfo(
            String date,
            int slotIndex,
            int voteCount,
            String percentage
    ) {}

    public record CreateMeetingResponse(
            String meetingCode,
            String title,
            String message
    ) {}

    public record VoteResponse(
            boolean success,
            String message
    ) {}

    public record ConfirmResponse(
            boolean success,
            String message,
            String confirmedDate,
            Integer confirmedSlotIndex
    ) {}

    public record ExportCalendarResponse(
            boolean success,
            String message,
            String icsBase64,
            String filename
    ) {}
}

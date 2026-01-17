package com.cover.time2gather.api.meeting;

import com.cover.time2gather.config.JpaAuditingConfig;
import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.meeting.service.CalendarExportService;
import com.cover.time2gather.domain.meeting.service.MeetingFacadeService;
import com.cover.time2gather.domain.meeting.service.MeetingSelectionService;
import com.cover.time2gather.domain.meeting.service.MeetingService;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.oauth.OidcProviderRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = MeetingController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
@AutoConfigureMockMvc(addFilters = false)
class MeetingControllerExportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeetingService meetingService;

    @MockitoBean
    private MeetingSelectionService selectionService;

    @MockitoBean
    private MeetingFacadeService meetingFacadeService;

    @MockitoBean
    private CalendarExportService calendarExportService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private OidcProviderRegistry oidcProviderRegistry;

    @MockitoBean
    private UserRepository userRepository;

    @Nested
    @DisplayName("GET /api/v1/meetings/{meetingCode}/export")
    class ExportToCalendar {

        @Test
        @DisplayName("파라미터 없이 호출하면 bestSlot 첫번째로 ICS 생성")
        void shouldExportWithBestSlotWhenNoParams() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Meeting meeting = createMeeting(meetingCode, SelectionType.TIME);
            MeetingDetailData detailData = createDetailDataWithBestSlots(meeting);
            byte[] icsContent = "BEGIN:VCALENDAR\nEND:VCALENDAR".getBytes();

            when(meetingService.getMeetingByCode(meetingCode)).thenReturn(meeting);
            when(meetingService.getMeetingDetailData(eq(meetingCode), any())).thenReturn(detailData);
            when(calendarExportService.createIcsFile(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(icsContent);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/{meetingCode}/export", meetingCode))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().exists("Content-Disposition"));
        }

        @Test
        @DisplayName("date와 slotIndex 파라미터로 특정 시간대 ICS 생성")
        void shouldExportWithSpecificDateAndSlotIndex() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            String date = "2024-02-15";
            int slotIndex = 9; // 09:00
            
            Meeting meeting = createMeeting(meetingCode, SelectionType.TIME);
            byte[] icsContent = "BEGIN:VCALENDAR\nEND:VCALENDAR".getBytes();

            when(meetingService.getMeetingByCode(meetingCode)).thenReturn(meeting);
            when(calendarExportService.createIcsFile(
                eq(meeting.getTitle()),
                eq(meeting.getDescription()),
                eq(date),
                eq("09:00"),
                eq(meeting.getTimezone()),
                eq(meeting.getIntervalMinutes())
            )).thenReturn(icsContent);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/{meetingCode}/export", meetingCode)
                    .param("date", date)
                    .param("slotIndex", String.valueOf(slotIndex)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"));
        }

        @Test
        @DisplayName("ALL_DAY 타입 (slotIndex=-1)으로 종일 일정 ICS 생성")
        void shouldExportAllDayEvent() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            String date = "2024-02-15";
            int slotIndex = -1; // ALL_DAY
            
            Meeting meeting = createMeeting(meetingCode, SelectionType.ALL_DAY);
            byte[] icsContent = "BEGIN:VCALENDAR\nEND:VCALENDAR".getBytes();

            when(meetingService.getMeetingByCode(meetingCode)).thenReturn(meeting);
            when(calendarExportService.createIcsFile(
                eq(meeting.getTitle()),
                eq(meeting.getDescription()),
                eq(date),
                eq("ALL_DAY"),
                eq(meeting.getTimezone()),
                eq(meeting.getIntervalMinutes())
            )).thenReturn(icsContent);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/{meetingCode}/export", meetingCode)
                    .param("date", date)
                    .param("slotIndex", String.valueOf(slotIndex)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"));
        }

        @Test
        @DisplayName("잘못된 date 형식이면 400 Bad Request 반환")
        void shouldReturn400WhenInvalidDateFormat() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            String invalidDate = "15-02-2024"; // 잘못된 형식
            int slotIndex = 9;

            Meeting meeting = createMeeting(meetingCode, SelectionType.TIME);
            when(meetingService.getMeetingByCode(meetingCode)).thenReturn(meeting);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/{meetingCode}/export", meetingCode)
                    .param("date", invalidDate)
                    .param("slotIndex", String.valueOf(slotIndex)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Meeting에 존재하지 않는 날짜면 400 Bad Request 반환")
        void shouldReturn400WhenDateNotInMeeting() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            String nonExistentDate = "2024-12-31"; // Meeting에 없는 날짜
            int slotIndex = 9;

            Meeting meeting = createMeeting(meetingCode, SelectionType.TIME);
            when(meetingService.getMeetingByCode(meetingCode)).thenReturn(meeting);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/{meetingCode}/export", meetingCode)
                    .param("date", nonExistentDate)
                    .param("slotIndex", String.valueOf(slotIndex)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("date만 있고 slotIndex가 없으면 400 Bad Request 반환")
        void shouldReturn400WhenOnlyDateProvided() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            String date = "2024-02-15";

            Meeting meeting = createMeeting(meetingCode, SelectionType.TIME);
            when(meetingService.getMeetingByCode(meetingCode)).thenReturn(meeting);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/{meetingCode}/export", meetingCode)
                    .param("date", date))
                .andExpect(status().isBadRequest());
        }
    }

    private Meeting createMeeting(String meetingCode, SelectionType selectionType) {
        Map<String, int[]> availableDates;
        if (selectionType == SelectionType.ALL_DAY) {
            availableDates = Map.of(
                "2024-02-15", new int[]{},
                "2024-02-16", new int[]{}
            );
        } else {
            availableDates = Map.of(
                "2024-02-15", new int[]{9, 10, 11}, // 09:00, 10:00, 11:00
                "2024-02-16", new int[]{14, 15}     // 14:00, 15:00
            );
        }
        
        return Meeting.create(
            meetingCode,
            "테스트 미팅",
            "테스트 설명",
            1L, // hostUserId
            "Asia/Seoul",
            selectionType,
            60, // intervalMinutes
            availableDates
        );
    }

    private MeetingDetailData createDetailDataWithBestSlots(Meeting meeting) {
        MeetingDetailData.BestSlot bestSlot = new MeetingDetailData.BestSlot(
            "2024-02-15",
            9, // slotIndex
            4, // count
            80.0 // percentage
        );
        
        MeetingDetailData.SummaryData summary = new MeetingDetailData.SummaryData(
            5,
            List.of(bestSlot)
        );
        
        return new MeetingDetailData(
            meeting,
            null, // host
            List.of(), // participants
            List.of(), // selections
            null, // schedule
            summary,
            false // isParticipated
        );
    }
}

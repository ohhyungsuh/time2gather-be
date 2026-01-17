package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.meeting.dto.ConfirmMeetingRequest;
import com.cover.time2gather.config.JpaAuditingConfig;
import com.cover.time2gather.config.security.JwtAuthentication;
import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.meeting.service.CalendarExportService;
import com.cover.time2gather.domain.meeting.service.MeetingFacadeService;
import com.cover.time2gather.domain.meeting.service.MeetingLocationService;
import com.cover.time2gather.domain.meeting.service.MeetingSelectionService;
import com.cover.time2gather.domain.meeting.service.MeetingService;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.oauth.OidcProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = MeetingController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
@AutoConfigureMockMvc(addFilters = false)
class MeetingControllerConfirmTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MeetingService meetingService;

    @MockitoBean
    private MeetingSelectionService selectionService;

    @MockitoBean
    private MeetingFacadeService meetingFacadeService;

    @MockitoBean
    private CalendarExportService calendarExportService;

    @MockitoBean
    private MeetingLocationService locationService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private OidcProviderRegistry oidcProviderRegistry;

    @MockitoBean
    private UserRepository userRepository;

    private void setAuthentication(Long userId, String username) {
        JwtAuthentication auth = new JwtAuthentication(userId, username);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("PUT /api/v1/meetings/{meetingCode}/confirm")
    class ConfirmMeeting {

        @Test
        @DisplayName("호스트가 유효한 날짜/슬롯으로 확정하면 200 OK")
        void shouldConfirmMeetingSuccessfully() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long hostUserId = 1L;
            LocalDate date = LocalDate.of(2024, 2, 15);
            Integer slotIndex = 9;

            Meeting meeting = createMeeting(meetingCode, hostUserId, SelectionType.TIME);
            ConfirmMeetingRequest request = new ConfirmMeetingRequest(date, slotIndex);

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            doNothing().when(meetingService).confirmMeeting(any(Meeting.class), eq(date), eq(slotIndex));
            setAuthentication(hostUserId, "host@test.com");

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{meetingCode}/confirm", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("호스트가 아닌 사용자가 확정하면 403 Forbidden")
        void shouldReturn403WhenNotHost() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long hostUserId = 1L;
            Long otherUserId = 2L;
            LocalDate date = LocalDate.of(2024, 2, 15);
            Integer slotIndex = 9;

            Meeting meeting = createMeeting(meetingCode, hostUserId, SelectionType.TIME);
            ConfirmMeetingRequest request = new ConfirmMeetingRequest(date, slotIndex);

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            setAuthentication(otherUserId, "other@test.com");

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{meetingCode}/confirm", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("이미 확정된 미팅을 재확정하면 400 Bad Request")
        void shouldReturn400WhenAlreadyConfirmed() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long hostUserId = 1L;
            LocalDate date = LocalDate.of(2024, 2, 15);
            Integer slotIndex = 9;

            Meeting meeting = createMeeting(meetingCode, hostUserId, SelectionType.TIME);
            ConfirmMeetingRequest request = new ConfirmMeetingRequest(date, slotIndex);

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            doThrow(new IllegalStateException("이미 확정된 미팅입니다."))
                .when(meetingService).confirmMeeting(any(Meeting.class), eq(date), eq(slotIndex));
            setAuthentication(hostUserId, "host@test.com");

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{meetingCode}/confirm", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 날짜로 확정하면 400 Bad Request")
        void shouldReturn400WhenInvalidDate() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long hostUserId = 1L;
            LocalDate invalidDate = LocalDate.of(2024, 12, 31);
            Integer slotIndex = 9;

            Meeting meeting = createMeeting(meetingCode, hostUserId, SelectionType.TIME);
            ConfirmMeetingRequest request = new ConfirmMeetingRequest(invalidDate, slotIndex);

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            doThrow(new IllegalArgumentException("유효하지 않은 날짜입니다."))
                .when(meetingService).confirmMeeting(any(Meeting.class), eq(invalidDate), eq(slotIndex));
            setAuthentication(hostUserId, "host@test.com");

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{meetingCode}/confirm", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("유효하지 않은 slotIndex로 확정하면 400 Bad Request")
        void shouldReturn400WhenInvalidSlotIndex() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long hostUserId = 1L;
            LocalDate date = LocalDate.of(2024, 2, 15);
            Integer invalidSlotIndex = 99;

            Meeting meeting = createMeeting(meetingCode, hostUserId, SelectionType.TIME);
            ConfirmMeetingRequest request = new ConfirmMeetingRequest(date, invalidSlotIndex);

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            doThrow(new IllegalArgumentException("유효하지 않은 슬롯 인덱스입니다."))
                .when(meetingService).confirmMeeting(any(Meeting.class), eq(date), eq(invalidSlotIndex));
            setAuthentication(hostUserId, "host@test.com");

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{meetingCode}/confirm", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ALL_DAY 타입 미팅 확정 시 slotIndex null로 성공")
        void shouldConfirmAllDayMeetingSuccessfully() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long hostUserId = 1L;
            LocalDate date = LocalDate.of(2024, 2, 15);

            Meeting meeting = createMeeting(meetingCode, hostUserId, SelectionType.ALL_DAY);
            ConfirmMeetingRequest request = new ConfirmMeetingRequest(date, null);

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            doNothing().when(meetingService).confirmMeeting(any(Meeting.class), eq(date), eq(null));
            setAuthentication(hostUserId, "host@test.com");

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{meetingCode}/confirm", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/meetings/{meetingCode}/confirm")
    class CancelConfirmation {

        @Test
        @DisplayName("호스트가 확정된 미팅을 취소하면 200 OK")
        void shouldCancelConfirmationSuccessfully() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long hostUserId = 1L;

            Meeting meeting = createMeeting(meetingCode, hostUserId, SelectionType.TIME);

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            doNothing().when(meetingService).cancelConfirmation(any(Meeting.class));
            setAuthentication(hostUserId, "host@test.com");

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{meetingCode}/confirm", meetingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("호스트가 아닌 사용자가 취소하면 403 Forbidden")
        void shouldReturn403WhenNotHost() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long hostUserId = 1L;
            Long otherUserId = 2L;

            Meeting meeting = createMeeting(meetingCode, hostUserId, SelectionType.TIME);

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            setAuthentication(otherUserId, "other@test.com");

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{meetingCode}/confirm", meetingCode))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("확정되지 않은 미팅을 취소하면 400 Bad Request")
        void shouldReturn400WhenNotConfirmed() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long hostUserId = 1L;

            Meeting meeting = createMeeting(meetingCode, hostUserId, SelectionType.TIME);

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            doThrow(new IllegalStateException("확정되지 않은 미팅입니다."))
                .when(meetingService).cancelConfirmation(any(Meeting.class));
            setAuthentication(hostUserId, "host@test.com");

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{meetingCode}/confirm", meetingCode))
                .andExpect(status().isBadRequest());
        }
    }

    private Meeting createMeeting(String meetingCode, Long hostUserId, SelectionType selectionType) {
        Map<String, int[]> availableDates;
        if (selectionType == SelectionType.ALL_DAY) {
            availableDates = Map.of(
                "2024-02-15", new int[]{},
                "2024-02-16", new int[]{}
            );
        } else {
            availableDates = Map.of(
                "2024-02-15", new int[]{9, 10, 11},
                "2024-02-16", new int[]{14, 15}
            );
        }

        return Meeting.create(
            meetingCode,
            "테스트 미팅",
            "테스트 설명",
            hostUserId,
            "Asia/Seoul",
            selectionType,
            60,
            availableDates
        );
    }
}

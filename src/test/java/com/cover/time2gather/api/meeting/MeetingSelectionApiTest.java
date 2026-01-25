package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.common.GlobalExceptionHandler;
import com.cover.time2gather.config.JpaAuditingConfig;
import com.cover.time2gather.config.security.JwtAuthentication;
import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.meeting.service.CalendarExportService;
import com.cover.time2gather.domain.meeting.service.MeetingFacadeService;
import com.cover.time2gather.domain.meeting.service.MeetingLocationService;
import com.cover.time2gather.domain.meeting.service.MeetingSelectionService;
import com.cover.time2gather.domain.meeting.service.MeetingService;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.oauth.OidcProviderRegistry;
import com.cover.time2gather.util.MessageProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = MeetingController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
@Import({GlobalExceptionHandler.class, MessageProvider.class})
@AutoConfigureMockMvc(addFilters = false)
class MeetingSelectionApiTest {

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
    @DisplayName("PUT /api/v1/meetings/{meetingCode}/selections")
    class UpsertUserSelections {

        @Test
        @DisplayName("확정된 미팅에서 투표를 수정하면 400 Bad Request")
        void shouldReturn400WhenMeetingAlreadyConfirmed() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long userId = 1L;

            Meeting meeting = createMeeting(meetingCode, 2L, SelectionType.TIME);

            String requestBody = """
                {
                    "selections": [
                        {
                            "date": "2024-02-15",
                            "type": "TIME",
                            "times": ["09:00", "10:00"]
                        }
                    ]
                }
                """;

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            doThrow(new BusinessException(ErrorCode.MEETING_ALREADY_CONFIRMED))
                .when(meetingFacadeService).upsertUserSelections(eq(meetingCode), eq(userId), any());
            setAuthentication(userId, "user@test.com");

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{meetingCode}/selections", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("미확정 미팅에서 투표 수정 성공")
        void shouldSucceedWhenMeetingNotConfirmed() throws Exception {
            // Given
            String meetingCode = "mtg_abc123";
            Long userId = 1L;

            Meeting meeting = createMeeting(meetingCode, 2L, SelectionType.TIME);

            String requestBody = """
                {
                    "selections": [
                        {
                            "date": "2024-02-15",
                            "type": "TIME",
                            "times": ["09:00", "10:00"]
                        }
                    ]
                }
                """;

            when(meetingService.getMeetingByCode(eq(meetingCode))).thenReturn(meeting);
            doNothing().when(meetingFacadeService).upsertUserSelections(eq(meetingCode), eq(userId), any());
            setAuthentication(userId, "user@test.com");

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{meetingCode}/selections", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
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

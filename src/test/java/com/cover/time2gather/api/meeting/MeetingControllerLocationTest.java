package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.meeting.dto.request.CreateMeetingRequest;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = MeetingController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
@AutoConfigureMockMvc(addFilters = false)
class MeetingControllerLocationTest {

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
    @DisplayName("POST /api/v1/meetings - 장소 투표 포함 생성")
    class CreateMeetingWithLocations {

        @Test
        @DisplayName("장소 투표 활성화 + 장소 2개로 미팅 생성 성공")
        void shouldCreateMeetingWithLocationsSuccessfully() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            Map<String, Object> request = Map.of(
                "title", "프로젝트 킥오프",
                "description", "킥오프 미팅입니다",
                "timezone", "Asia/Seoul",
                "selectionType", "TIME",
                "intervalMinutes", 60,
                "availableDates", Map.of("2024-02-15", new String[]{"09:00", "10:00"}),
                "locationVoteEnabled", true,
                "locations", List.of("강남역 스타벅스", "홍대 투썸")
            );

            Meeting meeting = Meeting.create(
                "mtg_abc123",
                "프로젝트 킥오프",
                "킥오프 미팅입니다",
                hostUserId,
                "Asia/Seoul",
                SelectionType.TIME,
                60,
                Map.of("2024-02-15", new int[]{9, 10}),
                true
            );

            when(meetingService.createMeeting(
                eq(hostUserId),
                anyString(),
                anyString(),
                anyString(),
                any(SelectionType.class),
                anyInt(),
                anyMap(),
                anyBoolean(),
                anyList()
            )).thenReturn(meeting);

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.meetingCode").value("mtg_abc123"));
        }

        @Test
        @DisplayName("장소 투표 활성화했지만 장소가 1개면 400 Bad Request")
        void shouldReturn400WhenLocationVoteEnabledButOnlyOneLocation() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            Map<String, Object> request = Map.of(
                "title", "프로젝트 킥오프",
                "timezone", "Asia/Seoul",
                "selectionType", "TIME",
                "intervalMinutes", 60,
                "availableDates", Map.of("2024-02-15", new String[]{"09:00", "10:00"}),
                "locationVoteEnabled", true,
                "locations", List.of("강남역 스타벅스")  // 1개만
            );

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("장소 투표 활성화했지만 장소가 없으면 400 Bad Request")
        void shouldReturn400WhenLocationVoteEnabledButNoLocations() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            Map<String, Object> request = Map.of(
                "title", "프로젝트 킥오프",
                "timezone", "Asia/Seoul",
                "selectionType", "TIME",
                "intervalMinutes", 60,
                "availableDates", Map.of("2024-02-15", new String[]{"09:00", "10:00"}),
                "locationVoteEnabled", true
                // locations 없음
            );

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("장소 투표 활성화하고 장소 6개면 400 Bad Request (최대 5개)")
        void shouldReturn400WhenMoreThan5Locations() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            Map<String, Object> request = Map.of(
                "title", "프로젝트 킥오프",
                "timezone", "Asia/Seoul",
                "selectionType", "TIME",
                "intervalMinutes", 60,
                "availableDates", Map.of("2024-02-15", new String[]{"09:00", "10:00"}),
                "locationVoteEnabled", true,
                "locations", List.of("장소1", "장소2", "장소3", "장소4", "장소5", "장소6")
            );

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("장소 투표 비활성화면 장소 없이도 성공")
        void shouldCreateMeetingWithoutLocationsWhenLocationVoteDisabled() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            Map<String, Object> request = Map.of(
                "title", "프로젝트 킥오프",
                "description", "킥오프 미팅입니다",
                "timezone", "Asia/Seoul",
                "selectionType", "TIME",
                "intervalMinutes", 60,
                "availableDates", Map.of("2024-02-15", new String[]{"09:00", "10:00"}),
                "locationVoteEnabled", false
            );

            Meeting meeting = Meeting.create(
                "mtg_abc123",
                "프로젝트 킥오프",
                "킥오프 미팅입니다",
                hostUserId,
                "Asia/Seoul",
                SelectionType.TIME,
                60,
                Map.of("2024-02-15", new int[]{9, 10}),
                false
            );

            when(meetingService.createMeeting(
                eq(hostUserId),
                anyString(),
                anyString(),
                anyString(),
                any(SelectionType.class),
                anyInt(),
                anyMap(),
                anyBoolean(),
                any()
            )).thenReturn(meeting);

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("장소 이름이 빈 문자열이면 400 Bad Request")
        void shouldReturn400WhenLocationNameIsEmpty() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            Map<String, Object> request = Map.of(
                "title", "프로젝트 킥오프",
                "timezone", "Asia/Seoul",
                "selectionType", "TIME",
                "intervalMinutes", 60,
                "availableDates", Map.of("2024-02-15", new String[]{"09:00", "10:00"}),
                "locationVoteEnabled", true,
                "locations", List.of("강남역 스타벅스", "")  // 빈 문자열
            );

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("장소 투표 활성화 + 장소 5개(최대)로 미팅 생성 성공")
        void shouldCreateMeetingWith5LocationsSuccessfully() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            Map<String, Object> request = Map.of(
                "title", "프로젝트 킥오프",
                "timezone", "Asia/Seoul",
                "selectionType", "TIME",
                "intervalMinutes", 60,
                "availableDates", Map.of("2024-02-15", new String[]{"09:00", "10:00"}),
                "locationVoteEnabled", true,
                "locations", List.of("장소1", "장소2", "장소3", "장소4", "장소5")
            );

            Meeting meeting = Meeting.create(
                "mtg_abc123",
                "프로젝트 킥오프",
                null,
                hostUserId,
                "Asia/Seoul",
                SelectionType.TIME,
                60,
                Map.of("2024-02-15", new int[]{9, 10}),
                true
            );

            when(meetingService.createMeeting(
                eq(hostUserId),
                anyString(),
                any(),
                anyString(),
                any(SelectionType.class),
                anyInt(),
                anyMap(),
                anyBoolean(),
                anyList()
            )).thenReturn(meeting);

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }
}

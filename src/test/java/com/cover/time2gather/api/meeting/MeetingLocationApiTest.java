package com.cover.time2gather.api.meeting;

import com.cover.time2gather.config.JpaAuditingConfig;
import com.cover.time2gather.config.security.JwtAuthentication;
import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingLocation;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = MeetingController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
@AutoConfigureMockMvc(addFilters = false)
class MeetingLocationApiTest {

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

    private Meeting createMeeting(Long hostUserId, boolean locationVoteEnabled) {
        return Meeting.create(
            "mtg_test123",
            "테스트 미팅",
            "설명",
            hostUserId,
            "Asia/Seoul",
            SelectionType.TIME,
            60,
            Map.of("2024-02-15", new int[]{9, 10}),
            locationVoteEnabled
        );
    }

    @Nested
    @DisplayName("POST /api/v1/meetings/{code}/locations - 장소 추가")
    class AddLocation {

        @Test
        @DisplayName("호스트가 장소를 추가하면 성공")
        void shouldAddLocationWhenHost() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";
            Meeting meeting = createMeeting(hostUserId, true);

            when(meetingService.getMeetingByCode(meetingCode)).thenReturn(meeting);

            MeetingLocation newLocation = MeetingLocation.create(1L, "새로운 장소", 2);
            when(locationService.addLocation(eq(meetingCode), eq(hostUserId), eq("새로운 장소")))
                .thenReturn(newLocation);

            Map<String, String> request = Map.of("name", "새로운 장소");

            // When & Then
            mockMvc.perform(post("/api/v1/meetings/{code}/locations", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("새로운 장소"));
        }

        @Test
        @DisplayName("호스트가 아닌 사용자가 장소를 추가하면 403")
        void shouldReturn403WhenNotHost() throws Exception {
            // Given
            Long hostUserId = 1L;
            Long otherUserId = 2L;
            setAuthentication(otherUserId, "other@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new org.springframework.security.access.AccessDeniedException("호스트만 장소를 추가할 수 있습니다."))
                .when(locationService).addLocation(eq(meetingCode), eq(otherUserId), any());

            Map<String, String> request = Map.of("name", "새로운 장소");

            // When & Then
            mockMvc.perform(post("/api/v1/meetings/{code}/locations", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("장소가 이미 5개면 400 Bad Request")
        void shouldReturn400WhenMaxLocationsReached() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new IllegalArgumentException("장소는 최대 5개까지 추가할 수 있습니다."))
                .when(locationService).addLocation(eq(meetingCode), eq(hostUserId), any());

            Map<String, String> request = Map.of("name", "새로운 장소");

            // When & Then
            mockMvc.perform(post("/api/v1/meetings/{code}/locations", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("장소 이름이 빈 문자열이면 400 Bad Request")
        void shouldReturn400WhenLocationNameEmpty() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new IllegalArgumentException("장소 이름은 비어있을 수 없습니다."))
                .when(locationService).addLocation(eq(meetingCode), eq(hostUserId), eq(""));

            Map<String, String> request = Map.of("name", "");

            // When & Then
            mockMvc.perform(post("/api/v1/meetings/{code}/locations", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/meetings/{code}/locations/{locationId} - 장소 삭제")
    class DeleteLocation {

        @Test
        @DisplayName("호스트가 장소를 삭제하면 성공")
        void shouldDeleteLocationWhenHost() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";
            Long locationId = 1L;

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{code}/locations/{locationId}", meetingCode, locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            verify(locationService).deleteLocation(eq(meetingCode), eq(hostUserId), eq(locationId));
        }

        @Test
        @DisplayName("호스트가 아닌 사용자가 장소를 삭제하면 403")
        void shouldReturn403WhenNotHost() throws Exception {
            // Given
            Long otherUserId = 2L;
            setAuthentication(otherUserId, "other@test.com");

            String meetingCode = "mtg_test123";
            Long locationId = 1L;

            doThrow(new org.springframework.security.access.AccessDeniedException("호스트만 장소를 삭제할 수 있습니다."))
                .when(locationService).deleteLocation(eq(meetingCode), eq(otherUserId), eq(locationId));

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{code}/locations/{locationId}", meetingCode, locationId))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("장소가 2개 이하일 때 삭제하면 400 Bad Request")
        void shouldReturn400WhenMinLocationsReached() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";
            Long locationId = 1L;

            doThrow(new IllegalArgumentException("장소는 최소 2개 이상이어야 합니다."))
                .when(locationService).deleteLocation(eq(meetingCode), eq(hostUserId), eq(locationId));

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{code}/locations/{locationId}", meetingCode, locationId))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 장소를 삭제하면 404")
        void shouldReturn404WhenLocationNotFound() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";
            Long locationId = 999L;

            doThrow(new IllegalArgumentException("장소를 찾을 수 없습니다."))
                .when(locationService).deleteLocation(eq(meetingCode), eq(hostUserId), eq(locationId));

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{code}/locations/{locationId}", meetingCode, locationId))
                .andExpect(status().isBadRequest());  // IllegalArgumentException은 400으로 처리
        }
    }
}

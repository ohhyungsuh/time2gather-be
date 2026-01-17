package com.cover.time2gather.api.meeting;

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

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = MeetingController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
@AutoConfigureMockMvc(addFilters = false)
class MeetingLocationConfirmApiTest {

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
    @DisplayName("PUT /api/v1/meetings/{code}/confirm-location - 장소 확정")
    class ConfirmLocation {

        @Test
        @DisplayName("호스트가 장소를 확정하면 성공")
        void shouldConfirmLocationWhenHost() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";
            Map<String, Object> request = Map.of("locationId", 1L);

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{code}/confirm-location", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            verify(locationService).confirmLocation(eq(meetingCode), eq(hostUserId), eq(1L));
        }

        @Test
        @DisplayName("호스트가 아닌 사용자가 장소를 확정하면 403")
        void shouldReturn403WhenNotHost() throws Exception {
            // Given
            Long otherUserId = 2L;
            setAuthentication(otherUserId, "other@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new org.springframework.security.access.AccessDeniedException("호스트만 장소를 확정할 수 있습니다."))
                .when(locationService).confirmLocation(eq(meetingCode), eq(otherUserId), eq(1L));

            Map<String, Object> request = Map.of("locationId", 1L);

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{code}/confirm-location", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("이미 장소가 확정된 경우 400")
        void shouldReturn400WhenAlreadyConfirmed() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new IllegalStateException("이미 장소가 확정되었습니다."))
                .when(locationService).confirmLocation(eq(meetingCode), eq(hostUserId), eq(1L));

            Map<String, Object> request = Map.of("locationId", 1L);

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{code}/confirm-location", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 장소를 확정하면 400")
        void shouldReturn400WhenLocationNotFound() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new IllegalArgumentException("장소를 찾을 수 없습니다."))
                .when(locationService).confirmLocation(eq(meetingCode), eq(hostUserId), eq(999L));

            Map<String, Object> request = Map.of("locationId", 999L);

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{code}/confirm-location", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/meetings/{code}/confirm-location - 장소 확정 취소")
    class CancelLocationConfirmation {

        @Test
        @DisplayName("호스트가 장소 확정을 취소하면 성공")
        void shouldCancelLocationConfirmationWhenHost() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{code}/confirm-location", meetingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            verify(locationService).cancelLocationConfirmation(eq(meetingCode), eq(hostUserId));
        }

        @Test
        @DisplayName("호스트가 아닌 사용자가 취소하면 403")
        void shouldReturn403WhenNotHost() throws Exception {
            // Given
            Long otherUserId = 2L;
            setAuthentication(otherUserId, "other@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new org.springframework.security.access.AccessDeniedException("호스트만 장소 확정을 취소할 수 있습니다."))
                .when(locationService).cancelLocationConfirmation(eq(meetingCode), eq(otherUserId));

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{code}/confirm-location", meetingCode))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("확정되지 않은 상태에서 취소하면 400")
        void shouldReturn400WhenNotConfirmed() throws Exception {
            // Given
            Long hostUserId = 1L;
            setAuthentication(hostUserId, "host@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new IllegalStateException("장소가 확정되지 않았습니다."))
                .when(locationService).cancelLocationConfirmation(eq(meetingCode), eq(hostUserId));

            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/{code}/confirm-location", meetingCode))
                .andExpect(status().isBadRequest());
        }
    }
}

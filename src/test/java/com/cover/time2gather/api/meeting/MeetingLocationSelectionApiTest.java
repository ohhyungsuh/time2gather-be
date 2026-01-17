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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = MeetingController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
@AutoConfigureMockMvc(addFilters = false)
class MeetingLocationSelectionApiTest {

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
    @DisplayName("PUT /api/v1/meetings/{code}/location-selections - 장소 투표")
    class VoteLocations {

        @Test
        @DisplayName("사용자가 장소에 투표하면 성공")
        void shouldVoteLocationsSuccessfully() throws Exception {
            // Given
            Long userId = 1L;
            setAuthentication(userId, "user@test.com");

            String meetingCode = "mtg_test123";
            Map<String, Object> request = Map.of("locationIds", List.of(1L, 2L));

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{code}/location-selections", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            verify(locationService).voteLocations(eq(meetingCode), eq(userId), eq(List.of(1L, 2L)));
        }

        @Test
        @DisplayName("빈 배열로 투표하면 기존 투표 삭제 (스킵)")
        void shouldClearVotesWhenEmptyArray() throws Exception {
            // Given
            Long userId = 1L;
            setAuthentication(userId, "user@test.com");

            String meetingCode = "mtg_test123";
            Map<String, Object> request = Map.of("locationIds", List.of());

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{code}/location-selections", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            verify(locationService).voteLocations(eq(meetingCode), eq(userId), eq(List.of()));
        }

        @Test
        @DisplayName("장소 투표가 비활성화된 미팅에서 투표하면 400")
        void shouldReturn400WhenLocationVoteDisabled() throws Exception {
            // Given
            Long userId = 1L;
            setAuthentication(userId, "user@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new IllegalArgumentException("장소 투표가 활성화되지 않은 미팅입니다."))
                .when(locationService).voteLocations(eq(meetingCode), eq(userId), any());

            Map<String, Object> request = Map.of("locationIds", List.of(1L));

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{code}/location-selections", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 장소에 투표하면 400")
        void shouldReturn400WhenLocationNotFound() throws Exception {
            // Given
            Long userId = 1L;
            setAuthentication(userId, "user@test.com");

            String meetingCode = "mtg_test123";

            doThrow(new IllegalArgumentException("존재하지 않는 장소입니다."))
                .when(locationService).voteLocations(eq(meetingCode), eq(userId), any());

            Map<String, Object> request = Map.of("locationIds", List.of(999L));

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/{code}/location-selections", meetingCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/meetings/{code}/location-selections - 내 장소 투표 조회")
    class GetMyLocationSelections {

        @Test
        @DisplayName("사용자의 장소 투표를 조회하면 성공")
        void shouldGetMyLocationSelectionsSuccessfully() throws Exception {
            // Given
            Long userId = 1L;
            setAuthentication(userId, "user@test.com");

            String meetingCode = "mtg_test123";

            when(locationService.selectUserLocationIds(eq(meetingCode), eq(userId)))
                .thenReturn(List.of(1L, 2L));

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/{code}/location-selections", meetingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.locationIds[0]").value(1))
                .andExpect(jsonPath("$.data.locationIds[1]").value(2));
        }

        @Test
        @DisplayName("투표하지 않은 경우 빈 배열 반환")
        void shouldReturnEmptyArrayWhenNoVotes() throws Exception {
            // Given
            Long userId = 1L;
            setAuthentication(userId, "user@test.com");

            String meetingCode = "mtg_test123";

            when(locationService.selectUserLocationIds(eq(meetingCode), eq(userId)))
                .thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/{code}/location-selections", meetingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.locationIds").isEmpty());
        }
    }
}

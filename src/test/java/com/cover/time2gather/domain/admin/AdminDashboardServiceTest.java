package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.domain.user.UserRole;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingRepository meetingRepository;

    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        adminDashboardService = new AdminDashboardService(userRepository, meetingRepository);
    }

    @Test
    void shouldReturnDashboardSummaryCounts() {
        when(userRepository.count()).thenReturn(12L);
        when(userRepository.countByProviderNot(User.AuthProvider.ANONYMOUS)).thenReturn(8L);
        when(userRepository.countByProvider(User.AuthProvider.ANONYMOUS)).thenReturn(4L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);
        when(meetingRepository.count()).thenReturn(20L);
        when(meetingRepository.countByIsActiveTrue()).thenReturn(15L);
        when(meetingRepository.countByConfirmedDateIsNotNull()).thenReturn(6L);

        AdminSummary summary = adminDashboardService.getSummary();

        assertThat(summary.users()).isEqualTo(new AdminSummary.UserStats(12L, 8L, 4L, 2L));
        assertThat(summary.meetings()).isEqualTo(new AdminSummary.MeetingStats(20L, 15L, 6L));
    }

    @Test
    void shouldSearchUsersByTrimmedKeywordWithBoundedPagination() {
        User user = User.builder()
                .username("Alice")
                .email("alice@example.com")
                .provider(User.AuthProvider.GOOGLE)
                .providerId("google-alice")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 3L);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 7, 21, 12, 0));
        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                org.mockito.ArgumentMatchers.eq("Alice"),
                org.mockito.ArgumentMatchers.eq("Alice"),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(user)));

        AdminPage<AdminUserItem> result = adminDashboardService.searchUsers("  Alice  ", 0, 20);

        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(3L);
            assertThat(item.username()).isEqualTo("Alice");
            assertThat(item.email()).isEqualTo("alice@example.com");
            assertThat(item.role()).isEqualTo(UserRole.USER);
        });
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                org.mockito.ArgumentMatchers.eq("Alice"),
                org.mockito.ArgumentMatchers.eq("Alice"),
                pageable.capture()
        );
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void shouldReturnMeetingHostWithoutPerMeetingQueries() {
        User host = User.builder()
                .username("Host")
                .provider(User.AuthProvider.KAKAO)
                .providerId("kakao-host")
                .build();
        ReflectionTestUtils.setField(host, "id", 7L);

        Meeting meeting = Meeting.create(
                "mtg_admin1",
                "Team Dinner",
                null,
                7L,
                "Asia/Seoul",
                SelectionType.ALL_DAY,
                60,
                Map.of("2026-07-25", new int[0])
        );
        ReflectionTestUtils.setField(meeting, "id", 9L);
        ReflectionTestUtils.setField(meeting, "createdAt", LocalDateTime.of(2026, 7, 21, 13, 0));

        when(meetingRepository.findByTitleContainingIgnoreCaseOrMeetingCodeContainingIgnoreCase(
                org.mockito.ArgumentMatchers.eq("Dinner"),
                org.mockito.ArgumentMatchers.eq("Dinner"),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(meeting)));
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(host));

        AdminPage<AdminMeetingItem> result = adminDashboardService.searchMeetings("Dinner", 0, 20);

        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(9L);
            assertThat(item.meetingCode()).isEqualTo("mtg_admin1");
            assertThat(item.hostUserId()).isEqualTo(7L);
            assertThat(item.hostUsername()).isEqualTo("Host");
        });
        verify(userRepository).findAllById(List.of(7L));
    }

    @Test
    void shouldRejectOversizedPagesAndLongQueries() {
        assertThatThrownBy(() -> adminDashboardService.searchUsers("", 0, 51))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adminDashboardService.searchMeetings("a".repeat(101), 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.domain.user.UserRole;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingUserSelectionRepository selectionRepository;

    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        adminDashboardService = new AdminDashboardService(userRepository, meetingRepository, selectionRepository);
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
            assertThat(item.participatedMeetings()).isEmpty();
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
    void shouldBatchParticipatedMeetingsByUserInRecentParticipationOrder() {
        User first = User.builder().username("Alice").provider(User.AuthProvider.GOOGLE).build();
        User second = User.builder().username("Bob").provider(User.AuthProvider.ANONYMOUS).build();
        User third = User.builder().username("Carol").provider(User.AuthProvider.KAKAO).build();
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        ReflectionTestUtils.setField(third, "id", 3L);
        Meeting older = meeting(10L, "Older", false);
        Meeting newer = meeting(20L, "Newer", true);
        when(userRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second, third)));
        when(selectionRepository.findAllByUserIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(
                selection(10L, 1L, 1), selection(20L, 1L, 3),
                selection(20L, 2L, 2), selection(99L, 1L, 4), selection(20L, 1L, 2)));
        when(meetingRepository.findAllByIdIn(List.of(10L, 20L, 99L))).thenReturn(List.of(older, newer));

        AdminPage<AdminUserItem> result = adminDashboardService.searchUsers("", 0, 20);

        assertThat(result.content().get(0).participatedMeetings())
                .extracting(AdminParticipatedMeetingItem::id).containsExactly(20L, 10L);
        assertThat(result.content().get(0).participatedMeetings().get(1).active()).isFalse();
        assertThat(result.content().get(1).participatedMeetings())
                .extracting(AdminParticipatedMeetingItem::id).containsExactly(20L);
        assertThat(result.content().get(2).participatedMeetings()).isEmpty();
        var response = com.cover.time2gather.api.admin.dto.AdminUserResponse.from(result.content().get(0));
        assertThat(response.participatedMeetings()).isEqualTo(result.content().get(0).participatedMeetings());
        verify(selectionRepository).findAllByUserIdIn(List.of(1L, 2L, 3L));
        verify(meetingRepository).findAllByIdIn(List.of(10L, 20L, 99L));
    }

    @Test
    void shouldSkipParticipationLookupForEmptyUserPage() {
        when(userRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(adminDashboardService.searchUsers("", 0, 20).content()).isEmpty();
        verifyNoInteractions(selectionRepository, meetingRepository);
    }

    private Meeting meeting(Long id, String title, boolean active) {
        Meeting meeting = Meeting.create("code_" + id, title, null, 9L, "Asia/Seoul",
                SelectionType.ALL_DAY, 60, Map.of("2026-07-25", new int[0]));
        ReflectionTestUtils.setField(meeting, "id", id);
        ReflectionTestUtils.setField(meeting, "isActive", active);
        return meeting;
    }

    @Test
    void exactCodeSearchAvoidsSubstringScanAndRetainsHostLookup() {
        Meeting item = meeting(12L, "Exact result", true);
        when(meetingRepository.findByMeetingCode("mtg_abcdefghij")).thenReturn(java.util.Optional.of(item));
        var result = adminDashboardService.searchMeetings("code:mtg_abcdefghij", 0, 20);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).extracting(AdminMeetingItem::id).containsExactly(12L);
        org.mockito.Mockito.verify(meetingRepository, org.mockito.Mockito.never())
                .findByTitleContainingIgnoreCaseOrMeetingCodeContainingIgnoreCase(
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cursorIsPassedToRepositoryAndNotLostWhenMappingUserResponse() {
        var pageable = org.springframework.data.domain.PageRequest.of(1, 20,
                org.springframework.data.domain.Sort.by("createdAt").descending()
                        .and(org.springframework.data.domain.Sort.by("id").descending()));
        when(userRepository.findAll(pageable, "cursor-one")).thenReturn(
                new com.cover.time2gather.domain.common.CursorPage<>(List.of(), pageable, 60, "cursor-two"));
        var result = adminDashboardService.searchUsers("", 1, 20, "cursor-one");
        var dto = com.cover.time2gather.api.admin.dto.AdminPageResponse.from(result,
                com.cover.time2gather.api.admin.dto.AdminUserResponse::from);
        assertThat(dto.nextCursor()).isEqualTo("cursor-two");
        assertThat(dto.totalElements()).isEqualTo(60);
        assertThatThrownBy(() -> adminDashboardService.searchUsers("Alice", 1, 20, "cursor-one"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmedFilterUsesRepositoryPaginationAndPreservesSearch() {
        var pageable = org.springframework.data.domain.PageRequest.of(1, 2,
                org.springframework.data.domain.Sort.by("createdAt").descending()
                        .and(org.springframework.data.domain.Sort.by("id").descending()));
        when(meetingRepository.findConfirmed("Dinner", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 2));
        var result = adminDashboardService.searchMeetings("  Dinner  ", 1, 2, null, true);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThatThrownBy(() -> adminDashboardService.searchMeetings("", 1, 2, "all-list-cursor", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exactCodeSearchAlsoAppliesConfirmedFilter() {
        Meeting item = meeting(12L, "Exact result", true);
        when(meetingRepository.findByMeetingCode("code_12")).thenReturn(java.util.Optional.of(item));
        assertThat(adminDashboardService.searchMeetings("code:code_12", 0, 20, null, true).totalElements()).isZero();
        ReflectionTestUtils.setField(item, "confirmedDate", java.time.LocalDate.of(2026, 7, 25));
        assertThat(adminDashboardService.searchMeetings("code:code_12", 0, 20, null, true).content())
                .extracting(AdminMeetingItem::id).containsExactly(12L);
    }

    private MeetingUserSelection selection(Long meetingId, Long userId, int day) {
        MeetingUserSelection selection = MeetingUserSelection.create(meetingId, userId,
                SelectionType.ALL_DAY, 60, Map.of("2026-07-25", new int[0]));
        ReflectionTestUtils.setField(selection, "updatedAt", LocalDateTime.of(2026, 7, day, 12, 0));
        return selection;
    }

    @Test
    void shouldRejectOversizedPagesAndLongQueries() {
        assertThatThrownBy(() -> adminDashboardService.searchUsers("", 0, 51))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adminDashboardService.searchMeetings("a".repeat(101), 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

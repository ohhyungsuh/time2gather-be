package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.domain.user.UserRole;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_QUERY_LENGTH = 100;

    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;

    public AdminSummary getSummary() {
        AdminSummary.UserStats users = new AdminSummary.UserStats(
                userRepository.count(),
                userRepository.countByProviderNot(User.AuthProvider.ANONYMOUS),
                userRepository.countByProvider(User.AuthProvider.ANONYMOUS),
                userRepository.countByRole(UserRole.ADMIN)
        );
        AdminSummary.MeetingStats meetings = new AdminSummary.MeetingStats(
                meetingRepository.count(),
                meetingRepository.countByIsActiveTrue(),
                meetingRepository.countByConfirmedDateIsNotNull()
        );
        return new AdminSummary(users, meetings);
    }

    public AdminPage<AdminUserItem> searchUsers(String query, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        Pageable pageable = createPageable(page, size);
        Page<User> users = normalizedQuery.isEmpty()
                ? userRepository.findAll(pageable)
                : userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        normalizedQuery,
                        normalizedQuery,
                        pageable
                );
        return AdminPage.from(users.map(AdminUserItem::from));
    }

    public AdminPage<AdminMeetingItem> searchMeetings(String query, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        Pageable pageable = createPageable(page, size);
        Page<Meeting> meetings = normalizedQuery.isEmpty()
                ? meetingRepository.findAll(pageable)
                : meetingRepository.findByTitleContainingIgnoreCaseOrMeetingCodeContainingIgnoreCase(
                        normalizedQuery,
                        normalizedQuery,
                        pageable
                );

        Map<Long, String> hostNames = getHostNames(meetings.getContent());
        return AdminPage.from(meetings.map(meeting ->
                AdminMeetingItem.from(meeting, hostNames.get(meeting.getHostUserId()))
        ));
    }

    private Pageable createPageable(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be at least 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending()));
    }

    private String normalizeQuery(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("query must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        return normalized;
    }

    private Map<Long, String> getHostNames(List<Meeting> meetings) {
        List<Long> hostUserIds = meetings.stream()
                .map(Meeting::getHostUserId)
                .distinct()
                .toList();
        if (hostUserIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> hostNames = new LinkedHashMap<>();
        userRepository.findAllById(hostUserIds)
                .forEach(user -> hostNames.put(user.getId(), user.getUsername()));
        return hostNames;
    }
}

package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.domain.user.UserRole;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
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
    private final MeetingUserSelectionRepository selectionRepository;

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
        return searchUsers(query, page, size, null);
    }

    public AdminPage<AdminUserItem> searchUsers(String query, int page, int size, String cursor) {
        String normalizedQuery = normalizeQuery(query);
        Pageable pageable = createPageable(page, size);
        validateCursorQuery(normalizedQuery, cursor);
        Page<User> users = normalizedQuery.isEmpty()
                ? (cursor == null ? userRepository.findAll(pageable) : userRepository.findAll(pageable, cursor))
                : userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        normalizedQuery,
                        normalizedQuery,
                        pageable
                );
        Map<Long, List<AdminParticipatedMeetingItem>> participatedMeetings = getParticipatedMeetings(users.getContent());
        return AdminPage.from(users.map(user -> AdminUserItem.from(
                user, participatedMeetings.getOrDefault(user.getId(), List.of()))));
    }

    private Map<Long, List<AdminParticipatedMeetingItem>> getParticipatedMeetings(List<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<MeetingUserSelection> selections = selectionRepository.findAllByUserIdIn(
                users.stream().map(User::getId).toList());
        List<Long> meetingIds = selections.stream().map(MeetingUserSelection::getMeetingId).distinct().toList();
        if (meetingIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Meeting> meetings = meetingRepository.findAllByIdIn(meetingIds).stream()
                .collect(Collectors.toMap(Meeting::getId, meeting -> meeting));
        Map<Long, List<MeetingUserSelection>> selectionsByUser = selections.stream()
                .collect(Collectors.groupingBy(MeetingUserSelection::getUserId));
        Map<Long, List<AdminParticipatedMeetingItem>> result = new LinkedHashMap<>();
        selectionsByUser.forEach((userId, userSelections) -> result.put(userId, userSelections.stream()
                .sorted(Comparator.comparing(MeetingUserSelection::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MeetingUserSelection::getMeetingId, Comparator.reverseOrder()))
                .map(MeetingUserSelection::getMeetingId)
                .distinct()
                .map(meetings::get)
                .filter(Objects::nonNull)
                .map(AdminParticipatedMeetingItem::from)
                .toList()));
        return result;
    }

    public AdminPage<AdminMeetingItem> searchMeetings(String query, int page, int size) {
        return searchMeetings(query, page, size, null);
    }

    public AdminPage<AdminMeetingItem> searchMeetings(String query, int page, int size, String cursor) {
        return searchMeetings(query, page, size, cursor, false);
    }

    public AdminPage<AdminMeetingItem> searchMeetings(String query, int page, int size, String cursor, boolean confirmedOnly) {
        String normalizedQuery = normalizeQuery(query);
        Pageable pageable = createPageable(page, size);
        validateCursorQuery(normalizedQuery, cursor);
        if (confirmedOnly && cursor != null) {
            throw new IllegalArgumentException("Confirmed results do not accept a list cursor");
        }
        Page<Meeting> meetings;
        if (normalizedQuery.regionMatches(true, 0, "code:", 0, 5)) {
            String code = normalizedQuery.substring(5).trim();
            if (!code.matches("[a-zA-Z0-9_-]{1,95}")) throw new IllegalArgumentException("Invalid meeting code");
            var exact = meetingRepository.findByMeetingCode(code)
                    .filter(meeting -> !confirmedOnly || meeting.isConfirmed());
            meetings = new PageImpl<>(page == 0 ? exact.stream().toList() : List.of(), pageable, exact.isPresent() ? 1 : 0);
        } else if (confirmedOnly) {
            meetings = meetingRepository.findConfirmed(normalizedQuery, pageable);
        } else {
            meetings = normalizedQuery.isEmpty()
                ? (cursor == null ? meetingRepository.findAll(pageable) : meetingRepository.findAll(pageable, cursor))
                : meetingRepository.findByTitleContainingIgnoreCaseOrMeetingCodeContainingIgnoreCase(
                        normalizedQuery,
                        normalizedQuery,
                        pageable
                );
        }

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

    private void validateCursorQuery(String query, String cursor) {
        if (cursor != null && !query.isEmpty()) {
            throw new IllegalArgumentException("Search results do not accept a list cursor");
        }
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

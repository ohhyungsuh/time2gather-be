package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUsageService {
    private static final int PAGE_SIZE = 100;
    private static final int ACTIVATION_THRESHOLD = 2;
    private final MeetingRepository meetingRepository;
    private final MeetingUserSelectionRepository selectionRepository;

    // Deliberately separate from the frequently refreshed summary: this reads
    // the stored meetings and responses, in bounded batches, on explicit request.
    public AdminUsageStats getUsage(Set<Long> excludedHostUserIds) {
        if (excludedHostUserIds == null || excludedHostUserIds.size() > 100
                || excludedHostUserIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("excludeHostUserIds must contain at most 100 positive IDs");
        }
        long total = 0, active = 0, responded = 0, activated = 0, confirmed = 0, externalResponses = 0;
        Map<Long, Long> meetingsByHost = new HashMap<>();
        PageRequest request = PageRequest.of(0, PAGE_SIZE,
                Sort.by("createdAt").descending().and(Sort.by("id").descending()));
        String cursor = null;
        while (true) {
            Page<Meeting> page = cursor == null ? meetingRepository.findAll(request)
                    : meetingRepository.findAll(request, cursor);
            List<Meeting> meetings = page.getContent().stream()
                    .filter(meeting -> !excludedHostUserIds.contains(meeting.getHostUserId())).toList();
            Map<Long, List<MeetingUserSelection>> byMeeting = meetings.isEmpty() ? Map.of()
                    : selectionRepository.findAllByMeetingIdIn(meetings.stream().map(Meeting::getId).toList())
                        .stream().collect(Collectors.groupingBy(MeetingUserSelection::getMeetingId));
            for (Meeting meeting : meetings) {
                total++;
                if (Boolean.TRUE.equals(meeting.getIsActive())) active++;
                if (meeting.isConfirmed()) confirmed++;
                meetingsByHost.merge(meeting.getHostUserId(), 1L, Long::sum);
                long responses = byMeeting.getOrDefault(meeting.getId(), List.of()).stream()
                        .filter(selection -> selection.getUserId() != null
                                && !Objects.equals(selection.getUserId(), meeting.getHostUserId()))
                        .filter(selection -> hasCurrentResponse(meeting, selection))
                        .map(MeetingUserSelection::getUserId).distinct().count();
                externalResponses += responses;
                if (responses > 0) responded++;
                if (responses >= ACTIVATION_THRESHOLD) activated++;
            }
            if (!page.hasNext()) break;
            cursor = page instanceof com.cover.time2gather.domain.common.CursorPage<?> cursorPage
                    ? cursorPage.getNextCursor() : null;
            request = request.next();
        }
        long hosts = meetingsByHost.size();
        long repeatHosts = meetingsByHost.values().stream().filter(count -> count >= 2).count();
        return new AdminUsageStats(Instant.now(), "CURRENT_STORED_STATE_ALL_TIME", ACTIVATION_THRESHOLD,
                total, active, responded, activated, confirmed, externalResponses, hosts, repeatHosts,
                ratio(activated, total), ratio(confirmed, total), ratio(repeatHosts, hosts));
    }

    private boolean hasCurrentResponse(Meeting meeting, MeetingUserSelection selection) {
        if ("NONE".equals(selection.availabilityResponse(meeting))) return true;
        if (selection.getSelections() == null || selection.getSelectionType() != meeting.getSelectionType()) return false;
        return selection.getSelections().entrySet().stream().anyMatch(entry -> {
            int[] candidates = meeting.getAvailableDates().get(entry.getKey());
            if (candidates == null) return false;
            if (meeting.getSelectionType() == SelectionType.ALL_DAY) return true;
            return entry.getValue() != null && Arrays.stream(entry.getValue())
                    .anyMatch(slot -> Arrays.stream(candidates).anyMatch(candidate -> candidate == slot));
        });
    }

    private double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0 : (double) numerator / denominator;
    }
}

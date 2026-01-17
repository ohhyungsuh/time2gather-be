package com.cover.time2gather.domain.chat;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 챗봇에서 사용하는 미팅 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingQueryService {

    private final MeetingRepository meetingRepository;
    private final MeetingUserSelectionRepository meetingUserSelectionRepository;

    /**
     * 사용자의 모든 미팅을 조회 (호스트 + 참여자)
     */
    public List<Meeting> findAllMeetingsByUser(Long userId) {
        // 호스트로 만든 미팅
        List<Meeting> hostedMeetings = meetingRepository.findByHostUserIdAndIsActiveTrue(userId);

        // 참여한 미팅 ID 목록
        List<Long> participatedMeetingIds = meetingUserSelectionRepository.findAllByUserId(userId)
            .stream()
            .map(MeetingUserSelection::getMeetingId)
            .collect(Collectors.toList());

        // 참여한 미팅 조회
        List<Meeting> participatedMeetings = participatedMeetingIds.isEmpty()
            ? List.of()
            : meetingRepository.findAllByIdInAndIsActiveTrue(participatedMeetingIds);

        // 중복 제거하며 합치기 (LinkedHashSet으로 순서 유지)
        Set<Meeting> allMeetings = new LinkedHashSet<>();
        allMeetings.addAll(hostedMeetings);
        allMeetings.addAll(participatedMeetings);

        return new ArrayList<>(allMeetings);
    }

    /**
     * 제목으로 미팅 검색
     */
    public List<Meeting> findMeetingsByTitle(Long userId, String keyword) {
        return findAllMeetingsByUser(userId).stream()
            .filter(meeting -> meeting.getTitle().toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * 다가오는 미팅 조회 (오늘 이후 날짜가 포함된 미팅)
     */
    public List<Meeting> findUpcomingMeetings(Long userId) {
        LocalDate today = LocalDate.now();
        return findAllMeetingsByUser(userId).stream()
            .filter(meeting -> hasUpcomingDate(meeting, today))
            .collect(Collectors.toList());
    }

    /**
     * 지난 미팅 조회 (모든 날짜가 오늘 이전인 미팅)
     */
    public List<Meeting> findPastMeetings(Long userId) {
        LocalDate today = LocalDate.now();
        return findAllMeetingsByUser(userId).stream()
            .filter(meeting -> !hasUpcomingDate(meeting, today))
            .collect(Collectors.toList());
    }

    /**
     * 미팅 코드로 사용자의 미팅 조회
     */
    public Meeting findMeetingByCode(Long userId, String meetingCode) {
        return findAllMeetingsByUser(userId).stream()
            .filter(meeting -> meeting.getMeetingCode().equals(meetingCode))
            .findFirst()
            .orElse(null);
    }

    private boolean hasUpcomingDate(Meeting meeting, LocalDate today) {
        if (meeting.getAvailableDates() == null) {
            return false;
        }
        return meeting.getAvailableDates().keySet().stream()
            .map(LocalDate::parse)
            .anyMatch(date -> !date.isBefore(today));
    }
}

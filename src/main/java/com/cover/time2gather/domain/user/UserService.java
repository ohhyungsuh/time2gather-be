package com.cover.time2gather.domain.user;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final MeetingRepository meetingRepository;
    private final MeetingUserSelectionRepository meetingUserSelectionRepository;

    /**
     * 사용자가 생성한 모임 목록 조회
     * - 최근 생성순(createdAt 기준)으로 정렬
     */
    public List<Meeting> getCreatedMeetings(Long userId) {
        return meetingRepository.findByHostUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 사용자가 참여한 모임 목록 조회
     * - 자신이 만든 모임도 참여했다면 포함
     * - 모든 참여 일정 노출 (기간 제한 없음)
     * - 최근 참여순(updatedAt 기준)으로 정렬
     */
    public List<Meeting> getParticipatedMeetings(Long userId) {
        return meetingUserSelectionRepository.findAllByUserId(userId)
                .stream()
                .sorted((s1, s2) -> s2.getUpdatedAt().compareTo(s1.getUpdatedAt())) // 최근 참여순 정렬
                .map(selection -> meetingRepository.findById(selection.getMeetingId()).orElse(null))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}


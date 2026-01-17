package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingLocation;
import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.ReportData;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.meeting.MeetingLocationRepository;
import com.cover.time2gather.infra.meeting.MeetingLocationSelectionRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportDataAggregator {

    private final MeetingRepository meetingRepository;
    private final MeetingUserSelectionRepository selectionRepository;
    private final MeetingLocationRepository locationRepository;
    private final MeetingLocationSelectionRepository locationSelectionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ReportData aggregate(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + meetingId));

        List<MeetingUserSelection> selections = selectionRepository.findAllByMeetingId(meetingId);

        // 장소 정보 조회 (장소 투표가 활성화된 경우에만)
        List<MeetingLocation> locations = Collections.emptyList();
        List<MeetingLocationSelection> locationSelections = Collections.emptyList();

        if (Boolean.TRUE.equals(meeting.getLocationVoteEnabled())) {
            locations = locationRepository.selectByMeetingIdOrderByDisplayOrderAsc(meetingId);
            locationSelections = locationSelectionRepository.selectByMeetingId(meetingId);
        }

        // 사용자 정보 조회 (시간 선택 + 장소 선택 참여자 모두 포함)
        Set<Long> userIds = new HashSet<>();
        selections.forEach(s -> userIds.add(s.getUserId()));
        locationSelections.forEach(s -> userIds.add(s.getUserId()));
        userIds.add(meeting.getHostUserId()); // 호스트도 포함

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return new ReportData(meeting, selections, userMap, locations, locationSelections);
    }
}

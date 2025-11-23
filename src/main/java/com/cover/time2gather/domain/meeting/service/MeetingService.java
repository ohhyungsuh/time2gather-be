package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private static final String CODE_PREFIX = "mtg_";
    private static final int CODE_LENGTH = 10;
    private static final String CODE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingUserSelectionRepository selectionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Meeting createMeeting(
            Long hostUserId,
            String title,
            String description,
            String timezone,
            Map<String, int[]> availableDates
    ) {
        // 사용자가 존재하는지 검증
        if (!userRepository.existsById(hostUserId)) {
            throw new IllegalArgumentException("User not found");
        }

        String meetingCode = generateUniqueMeetingCode();

        Meeting meeting = Meeting.create(
                meetingCode,
                title,
                description,
                hostUserId,
                timezone,
                availableDates
        );

        return meetingRepository.save(meeting);
    }

    public Meeting getMeetingByCode(String meetingCode) {
        return meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));
    }

    /**
     * 모임 상세 데이터 조회 (비즈니스 로직 포함)
     * 도메인 모델만 반환
     */
    public MeetingDetailData getMeetingDetailData(String meetingCode) {
        Meeting meeting = getMeetingByCode(meetingCode);
        List<MeetingUserSelection> selections = selectionRepository.findAllByMeetingId(meeting.getId());

        // 참여자 ID 목록 추출
        Set<Long> participantIds = selections.stream()
                .map(MeetingUserSelection::getUserId)
                .collect(Collectors.toSet());
        participantIds.add(meeting.getHostUserId()); // 방장도 포함

        // 한 번에 모든 사용자 정보 조회
        Map<Long, User> userMap = userRepository.findAllById(participantIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        User host = userMap.get(meeting.getHostUserId());
        List<User> participants = participantIds.stream()
                .map(userMap::get)
                .collect(Collectors.toList());

        // Schedule 데이터 구성
        MeetingDetailData.ScheduleData schedule = buildScheduleData(selections, userMap);

        // Summary 데이터 구성
        MeetingDetailData.SummaryData summary = buildSummaryData(selections, participantIds.size());

        return new MeetingDetailData(meeting, host, participants, selections, schedule, summary);
    }

    /**
     * 날짜/시간별 참여자 목록 구성 (비즈니스 로직)
     */
    private MeetingDetailData.ScheduleData buildScheduleData(
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap
    ) {
        Map<String, Map<Integer, List<User>>> dateTimeUserMap = new HashMap<>();

        for (MeetingUserSelection selection : selections) {
            User user = userMap.get(selection.getUserId());
            Map<String, int[]> userSelections = selection.getSelections();

            for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                String date = entry.getKey();
                int[] slots = entry.getValue();

                dateTimeUserMap.putIfAbsent(date, new HashMap<>());
                Map<Integer, List<User>> slotUserMap = dateTimeUserMap.get(date);

                for (int slot : slots) {
                    slotUserMap.putIfAbsent(slot, new ArrayList<>());
                    slotUserMap.get(slot).add(user);
                }
            }
        }

        return new MeetingDetailData.ScheduleData(dateTimeUserMap);
    }

    /**
     * 요약 정보 구성 (비즈니스 로직)
     */
    private MeetingDetailData.SummaryData buildSummaryData(
            List<MeetingUserSelection> selections,
            int totalParticipants
    ) {
        // 날짜-시간별 카운트
        Map<String, Map<Integer, Integer>> countMap = new HashMap<>();

        for (MeetingUserSelection selection : selections) {
            Map<String, int[]> userSelections = selection.getSelections();
            for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                String date = entry.getKey();
                int[] slots = entry.getValue();

                countMap.putIfAbsent(date, new HashMap<>());
                Map<Integer, Integer> slotCountMap = countMap.get(date);

                for (int slot : slots) {
                    slotCountMap.put(slot, slotCountMap.getOrDefault(slot, 0) + 1);
                }
            }
        }

        // bestSlots 찾기 (가장 많은 사람이 가능한 시간대)
        List<MeetingDetailData.BestSlot> bestSlots = new ArrayList<>();
        int maxCount = 0;

        for (Map.Entry<String, Map<Integer, Integer>> dateEntry : countMap.entrySet()) {
            String date = dateEntry.getKey();
            Map<Integer, Integer> slotCountMap = dateEntry.getValue();

            for (Map.Entry<Integer, Integer> slotEntry : slotCountMap.entrySet()) {
                int slot = slotEntry.getKey();
                int count = slotEntry.getValue();

                if (count > maxCount) {
                    maxCount = count;
                    bestSlots.clear();
                    bestSlots.add(new MeetingDetailData.BestSlot(
                            date,
                            slot,
                            count,
                            totalParticipants > 0 ? (count * 100.0 / totalParticipants) : 0
                    ));
                } else if (count == maxCount) {
                    bestSlots.add(new MeetingDetailData.BestSlot(
                            date,
                            slot,
                            count,
                            totalParticipants > 0 ? (count * 100.0 / totalParticipants) : 0
                    ));
                }
            }
        }

        return new MeetingDetailData.SummaryData(totalParticipants, bestSlots);
    }

    private String generateUniqueMeetingCode() {
        String code;
        do {
            code = CODE_PREFIX + generateRandomString(CODE_LENGTH);
        } while (meetingRepository.existsByMeetingCode(code));
        return code;
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}

package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.MeetingLocation;
import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import com.cover.time2gather.infra.meeting.MeetingLocationRepository;
import com.cover.time2gather.infra.meeting.MeetingLocationSelectionRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
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
    private final MeetingLocationRepository locationRepository;
    private final MeetingLocationSelectionRepository locationSelectionRepository;
    private final UserRepository userRepository;
    private final MeetingUserSelectionRepository selectionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Meeting createMeeting(
            Long hostUserId,
            String title,
            String description,
            String timezone,
            com.cover.time2gather.domain.meeting.SelectionType selectionType,
            Integer intervalMinutes,
            Map<String, int[]> availableDates
    ) {
        return createMeeting(hostUserId, title, description, timezone, selectionType, 
                intervalMinutes, availableDates, false, null);
    }

    @Transactional
    public Meeting createMeeting(
            Long hostUserId,
            String title,
            String description,
            String timezone,
            com.cover.time2gather.domain.meeting.SelectionType selectionType,
            Integer intervalMinutes,
            Map<String, int[]> availableDates,
            Boolean locationVoteEnabled,
            List<String> locations
    ) {
        // 사용자가 존재하는지 검증
        if (!userRepository.existsById(hostUserId)) {
            throw new IllegalArgumentException("User not found");
        }

        // 장소 투표 활성화 시 검증
        boolean enableLocationVote = Boolean.TRUE.equals(locationVoteEnabled);
        if (enableLocationVote) {
            validateLocations(locations);
        }

        String meetingCode = generateUniqueMeetingCode();

        Meeting meeting = Meeting.create(
                meetingCode,
                title,
                description,
                hostUserId,
                timezone,
                selectionType,
                intervalMinutes,
                availableDates,
                enableLocationVote
        );

        Meeting savedMeeting = meetingRepository.save(meeting);

        // 장소 후보 저장
        if (enableLocationVote && locations != null) {
            for (int i = 0; i < locations.size(); i++) {
                MeetingLocation location = MeetingLocation.create(
                        savedMeeting.getId(),
                        locations.get(i),
                        i  // displayOrder
                );
                locationRepository.save(location);
            }
        }

        return savedMeeting;
    }

    private void validateLocations(List<String> locations) {
        if (locations == null || locations.size() < 2) {
            throw new IllegalArgumentException("장소 투표를 활성화하려면 최소 2개의 장소가 필요합니다.");
        }
        if (locations.size() > 5) {
            throw new IllegalArgumentException("장소는 최대 5개까지 추가할 수 있습니다.");
        }
        for (String location : locations) {
            if (location == null || location.trim().isEmpty()) {
                throw new IllegalArgumentException("장소 이름은 비어있을 수 없습니다.");
            }
        }
    }

    public Meeting getMeetingByCode(String meetingCode) {
        return meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));
    }

    @Transactional
    public void confirmMeeting(Meeting meeting, LocalDate date, Integer slotIndex) {
        meeting.confirm(date, slotIndex);
        meetingRepository.save(meeting);
    }

    @Transactional
    public void cancelConfirmation(Meeting meeting) {
        meeting.cancelConfirmation();
        meetingRepository.save(meeting);
    }

    /**
     * 모임 상세 데이터 조회 (비즈니스 로직 포함)
     * 도메인 모델만 반환
     */
    public MeetingDetailData getMeetingDetailData(String meetingCode, Long currentUserId) {
        Meeting meeting = getMeetingByCode(meetingCode);
        List<MeetingUserSelection> selections = selectionRepository.findAllByMeetingId(meeting.getId());

        // 참여자 ID 목록 추출 (시간 선택이 있는 사용자만)
        Set<Long> participantIds = selections.stream()
                .map(MeetingUserSelection::getUserId)
                .collect(Collectors.toSet());

        // 한 번에 모든 사용자 정보 조회
        Map<Long, User> userMap = userRepository.findAllById(participantIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 방장 정보 조회 (참여자 목록과 별도)
        User host = userRepository.findById(meeting.getHostUserId())
                .orElseThrow(() -> new IllegalArgumentException("Host not found"));

        List<User> participants = participantIds.stream()
                .map(userMap::get)
                .collect(Collectors.toList());

        // 현재 사용자의 참여 여부 확인
        boolean isParticipated = currentUserId != null && participantIds.contains(currentUserId);

        // Schedule 데이터 구성
        MeetingDetailData.ScheduleData schedule = buildScheduleData(selections, userMap);

        // Summary 데이터 구성 (참여자 수는 시간 선택이 있는 사용자만)
        MeetingDetailData.SummaryData summary = buildSummaryData(meeting, selections, participantIds.size());

        // 장소 데이터 구성
        MeetingDetailData.LocationData locationData = buildLocationData(meeting, userMap);

        return new MeetingDetailData(meeting, host, participants, selections, schedule, summary, isParticipated, locationData);
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

                // ALL_DAY 타입 (빈 배열)인 경우
                if (slots.length == 0) {
                    slotUserMap.putIfAbsent(-1, new ArrayList<>());
                    slotUserMap.get(-1).add(user);
                } else {
                    // TIME 타입 (슬롯 배열)인 경우
                    for (int slot : slots) {
                        slotUserMap.putIfAbsent(slot, new ArrayList<>());
                        slotUserMap.get(slot).add(user);
                    }
                }
            }
        }

        return new MeetingDetailData.ScheduleData(dateTimeUserMap);
    }

    /**
     * 요약 정보 구성 (비즈니스 로직)
     */
    private MeetingDetailData.SummaryData buildSummaryData(
            Meeting meeting,
            List<MeetingUserSelection> selections,
            int totalParticipants
    ) {
        boolean isAllDay = meeting.getSelectionType() == com.cover.time2gather.domain.meeting.SelectionType.ALL_DAY;

        // 날짜-시간별 카운트 (ALL_DAY는 날짜별, TIME은 시간별)
        Map<String, Map<Integer, Integer>> countMap = new HashMap<>();

        for (MeetingUserSelection selection : selections) {
            Map<String, int[]> userSelections = selection.getSelections();
            for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                String date = entry.getKey();
                int[] slots = entry.getValue();

                countMap.putIfAbsent(date, new HashMap<>());
                Map<Integer, Integer> slotCountMap = countMap.get(date);

                if (isAllDay) {
                    // ALL_DAY: 날짜별로 카운트 (슬롯 인덱스 -1 사용)
                    slotCountMap.put(-1, slotCountMap.getOrDefault(-1, 0) + 1);
                } else {
                    // TIME: 시간별로 카운트
                    for (int slot : slots) {
                        slotCountMap.put(slot, slotCountMap.getOrDefault(slot, 0) + 1);
                    }
                }
            }
        }

        // bestSlots 찾기 (투표 수가 많은 순서대로 TOP 3)
        List<MeetingDetailData.BestSlot> allSlots = new ArrayList<>();

        // 모든 날짜/슬롯을 리스트로 수집
        for (Map.Entry<String, Map<Integer, Integer>> dateEntry : countMap.entrySet()) {
            String date = dateEntry.getKey();
            Map<Integer, Integer> slotCountMap = dateEntry.getValue();

            for (Map.Entry<Integer, Integer> slotEntry : slotCountMap.entrySet()) {
                int slot = slotEntry.getKey();
                int count = slotEntry.getValue();

                allSlots.add(new MeetingDetailData.BestSlot(
                        date,
                        slot,
                        count,
                        totalParticipants > 0 ? (count * 100.0 / totalParticipants) : 0
                ));
            }
        }

        // 정렬: 1) 투표 수 내림차순, 2) 날짜 오름차순 (가까운 날짜 우선)
        List<MeetingDetailData.BestSlot> bestSlots = allSlots.stream()
                .sorted((a, b) -> {
                    // 투표 수 비교 (내림차순)
                    int countCompare = Integer.compare(b.getCount(), a.getCount());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    // 투표 수가 같으면 날짜 비교 (오름차순 - 가까운 날짜 우선)
                    int dateCompare = a.getDate().compareTo(b.getDate());
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    // 날짜도 같으면 슬롯 인덱스 비교 (오름차순 - 이른 시간 우선)
                    return Integer.compare(a.getSlotIndex(), b.getSlotIndex());
                })
                .limit(3)  // TOP 3만 선택
                .collect(Collectors.toList());

        return new MeetingDetailData.SummaryData(totalParticipants, bestSlots);
    }

    /**
     * 장소 투표 데이터 구성
     */
    private MeetingDetailData.LocationData buildLocationData(Meeting meeting, Map<Long, User> userMap) {
        if (!meeting.isLocationVoteEnabled()) {
            return null;
        }

        // 1. 장소 목록 조회
        List<MeetingLocation> locations = locationRepository.selectByMeetingIdOrderByDisplayOrderAsc(meeting.getId());

        // 2. 장소별 투표 조회
        List<MeetingLocationSelection> locationSelections = locationSelectionRepository.selectByMeetingId(meeting.getId());

        // 3. 장소별 투표 수 및 투표자 집계
        Map<Long, List<User>> locationVotersMap = new HashMap<>();
        for (MeetingLocationSelection selection : locationSelections) {
            Long locationId = selection.getLocationId();
            locationVotersMap.putIfAbsent(locationId, new ArrayList<>());

            User voter = userMap.get(selection.getUserId());
            if (voter != null) {
                locationVotersMap.get(locationId).add(voter);
            }
        }

        // 4. 총 투표자 수 (중복 제거)
        Set<Long> uniqueVoterIds = locationSelections.stream()
                .map(MeetingLocationSelection::getUserId)
                .collect(Collectors.toSet());
        int totalVoters = uniqueVoterIds.size();

        // 5. LocationInfo 목록 생성
        List<MeetingDetailData.LocationInfo> locationInfos = new ArrayList<>();
        MeetingDetailData.LocationInfo confirmedLocation = null;

        for (MeetingLocation location : locations) {
            List<User> voters = locationVotersMap.getOrDefault(location.getId(), new ArrayList<>());
            int voteCount = voters.size();
            double percentageValue = totalVoters > 0 ? (voteCount * 100.0 / totalVoters) : 0;
            String percentage = Math.round(percentageValue) + "%";

            MeetingDetailData.LocationInfo locationInfo = new MeetingDetailData.LocationInfo(
                    location.getId(),
                    location.getName(),
                    location.getDisplayOrder(),
                    voteCount,
                    percentage,
                    voters
            );

            locationInfos.add(locationInfo);

            // 확정된 장소 찾기
            if (meeting.isLocationConfirmed() && location.getId().equals(meeting.getConfirmedLocationId())) {
                confirmedLocation = locationInfo;
            }
        }

        return new MeetingDetailData.LocationData(true, locationInfos, confirmedLocation);
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

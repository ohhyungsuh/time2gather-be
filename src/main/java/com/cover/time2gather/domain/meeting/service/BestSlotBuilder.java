package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.user.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BestSlot 연속 시간 병합 빌더
 * 
 * 연속된 시간 슬롯을 하나로 병합하고, 각 슬롯에 대한 참여자 정보를 제공합니다.
 * 
 * 병합 정책:
 * - 2개 이상 연속된 슬롯은 하나로 병합
 * - 병합된 슬롯의 count는 모든 슬롯에 참여한 인원만 카운트 (엄격)
 * - 각 BestSlot에 참여자 명단 포함
 */
@Component
public class BestSlotBuilder {

    private static final int TOP_N = 3;

    /**
     * 요약 데이터 생성 (연속 슬롯 병합 적용)
     */
    public MeetingDetailData.SummaryData buildSummaryData(
            Meeting meeting,
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap,
            int totalParticipants
    ) {
        boolean isAllDay = meeting.getSelectionType() == SelectionType.ALL_DAY;

        if (isAllDay) {
            return buildAllDaySummary(selections, userMap, totalParticipants);
        }

        return buildTimeSummary(selections, userMap, totalParticipants);
    }

    /**
     * TIME 타입: 모든 연속 범위 조합 평가 후 겹치지 않는 Top3 선택
     * 
     * 1. 모든 가능한 연속 범위 조합을 생성하고 공통 참여자 계산 (Incremental intersection)
     * 2. count 기준 정렬 후, 같은 날짜에서 겹치는 범위는 제외하고 Top3 선택
     * 
     * 시간복잡도: O(D × S² × U) - Incremental intersection 최적화 적용
     * - D: 날짜 수, S: 슬롯 수, U: 사용자 수
     */
    private MeetingDetailData.SummaryData buildTimeSummary(
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap,
            int totalParticipants
    ) {
        // 1. 날짜-슬롯별 참여자 Set 생성
        Map<String, Map<Integer, Set<Long>>> dateSlotUsersMap = buildDateSlotUsersMap(selections);

        // 2. 모든 연속 범위 조합 평가 (Incremental intersection으로 O(S² × U) 달성)
        List<MeetingDetailData.BestSlot> allSlots = new ArrayList<>();

        for (Map.Entry<String, Map<Integer, Set<Long>>> dateEntry : dateSlotUsersMap.entrySet()) {
            String date = dateEntry.getKey();
            Map<Integer, Set<Long>> slotUsersMap = dateEntry.getValue();

            // 해당 날짜의 모든 연속 범위에 대해 BestSlot 생성
            List<MeetingDetailData.BestSlot> dateSlots = generateSlotsWithIncrementalIntersection(
                    date, slotUsersMap, userMap, totalParticipants);
            allSlots.addAll(dateSlots);
        }

        // 3. 정렬: count 내림차순 → 범위 넓이 내림차순 → 날짜 오름차순 → startSlotIndex 오름차순
        List<MeetingDetailData.BestSlot> sortedSlots = allSlots.stream()
                .sorted(Comparator
                        .<MeetingDetailData.BestSlot>comparingInt(MeetingDetailData.BestSlot::getCount).reversed()
                        .thenComparing(Comparator.<MeetingDetailData.BestSlot>comparingInt(s -> s.getEndSlotIndex() - s.getStartSlotIndex()).reversed())
                        .thenComparing(MeetingDetailData.BestSlot::getDate)
                        .thenComparingInt(MeetingDetailData.BestSlot::getStartSlotIndex))
                .collect(Collectors.toList());

        // 4. Top3 선택 (같은 날짜에서 겹치는 범위는 제외)
        List<MeetingDetailData.BestSlot> bestSlots = selectNonOverlappingTopN(sortedSlots, TOP_N);

        return new MeetingDetailData.SummaryData(totalParticipants, bestSlots);
    }

    /**
     * Incremental intersection을 사용하여 모든 연속 범위의 BestSlot 생성
     * 
     * 기존 방식: generateAllConsecutiveRanges() + findCommonUsers() → O(S³ × U)
     * 최적화 방식: 확장하면서 교집합 누적 유지 → O(S² × U)
     * 
     * @param date 날짜
     * @param slotUsersMap 슬롯별 참여자 맵
     * @param userMap 사용자 ID → User 맵
     * @param totalParticipants 전체 참여자 수
     * @return 해당 날짜의 모든 BestSlot 리스트
     */
    private List<MeetingDetailData.BestSlot> generateSlotsWithIncrementalIntersection(
            String date,
            Map<Integer, Set<Long>> slotUsersMap,
            Map<Long, User> userMap,
            int totalParticipants
    ) {
        if (slotUsersMap.isEmpty()) {
            return List.of();
        }

        List<Integer> sortedSlots = slotUsersMap.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        List<MeetingDetailData.BestSlot> result = new ArrayList<>();

        // 각 시작점에서 연속 확장하면서 교집합 누적
        for (int i = 0; i < sortedSlots.size(); i++) {
            int start = sortedSlots.get(i);
            Set<Long> commonUserIds = new HashSet<>(slotUsersMap.get(start));
            int end = start;

            // 단일 슬롯 (start == end) 추가
            addBestSlotIfValid(result, date, start, end, commonUserIds, userMap, totalParticipants);

            // 연속된 슬롯으로 확장하면서 교집합 유지
            for (int j = i + 1; j < sortedSlots.size(); j++) {
                int next = sortedSlots.get(j);

                if (next != end + 1) {
                    // 연속이 끊김 → 이 시작점에서의 확장 종료
                    break;
                }

                // 연속됨 → 확장하면서 교집합 갱신
                end = next;
                Set<Long> nextSlotUsers = slotUsersMap.get(next);
                commonUserIds.retainAll(nextSlotUsers);

                // 교집합이 비어있으면 더 확장해도 의미 없음 (Early termination)
                if (commonUserIds.isEmpty()) {
                    break;
                }

                // 확장된 범위 추가
                addBestSlotIfValid(result, date, start, end, commonUserIds, userMap, totalParticipants);
            }
        }

        return result;
    }

    /**
     * 유효한 BestSlot을 결과 리스트에 추가
     */
    private void addBestSlotIfValid(
            List<MeetingDetailData.BestSlot> result,
            String date,
            int start,
            int end,
            Set<Long> commonUserIds,
            Map<Long, User> userMap,
            int totalParticipants
    ) {
        if (commonUserIds.isEmpty()) {
            return;
        }

        // Set을 복사하여 사용 (이후 retainAll로 원본이 변경되므로)
        List<User> participants = commonUserIds.stream()
                .map(userMap::get)
                .filter(u -> u != null)
                .collect(Collectors.toList());

        int count = participants.size();
        if (count == 0) {
            return;
        }

        double percentage = totalParticipants > 0 ? (count * 100.0 / totalParticipants) : 0;

        result.add(new MeetingDetailData.BestSlot(
                date,
                start,
                end,
                count,
                percentage,
                participants
        ));
    }

    /**
     * 같은 날짜에서 겹치는 범위를 제외하고 Top N 선택
     */
    private List<MeetingDetailData.BestSlot> selectNonOverlappingTopN(
            List<MeetingDetailData.BestSlot> sortedSlots, 
            int n
    ) {
        List<MeetingDetailData.BestSlot> result = new ArrayList<>();
        // 날짜별로 선택된 범위 추적
        Map<String, List<SlotRange>> selectedRangesByDate = new HashMap<>();

        for (MeetingDetailData.BestSlot slot : sortedSlots) {
            if (result.size() >= n) {
                break;
            }

            String date = slot.getDate();
            SlotRange currentRange = new SlotRange(slot.getStartSlotIndex(), slot.getEndSlotIndex());

            // 해당 날짜에서 이미 선택된 범위와 겹치는지 확인
            List<SlotRange> selectedRanges = selectedRangesByDate.getOrDefault(date, new ArrayList<>());
            boolean overlaps = selectedRanges.stream()
                    .anyMatch(selected -> rangesOverlap(selected, currentRange));

            if (!overlaps) {
                result.add(slot);
                selectedRanges.add(currentRange);
                selectedRangesByDate.put(date, selectedRanges);
            }
        }

        return result;
    }

    /**
     * 두 범위가 겹치는지 확인
     */
    private boolean rangesOverlap(SlotRange a, SlotRange b) {
        return a.start <= b.end && b.start <= a.end;
    }

    /**
     * ALL_DAY 타입: 병합 없이 날짜별 처리
     */
    private MeetingDetailData.SummaryData buildAllDaySummary(
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap,
            int totalParticipants
    ) {
        // 날짜별 참여자 수집
        Map<String, Set<Long>> dateUsersMap = new HashMap<>();

        for (MeetingUserSelection selection : selections) {
            for (String date : selection.getSelections().keySet()) {
                dateUsersMap.computeIfAbsent(date, k -> new HashSet<>())
                        .add(selection.getUserId());
            }
        }

        // BestSlot 생성
        List<MeetingDetailData.BestSlot> allSlots = new ArrayList<>();

        for (Map.Entry<String, Set<Long>> entry : dateUsersMap.entrySet()) {
            String date = entry.getKey();
            Set<Long> userIds = entry.getValue();

            List<User> participants = userIds.stream()
                    .map(userMap::get)
                    .filter(u -> u != null)
                    .collect(Collectors.toList());

            int count = participants.size();
            double percentage = totalParticipants > 0 ? (count * 100.0 / totalParticipants) : 0;

            // ALL_DAY는 slotIndex = -1
            allSlots.add(new MeetingDetailData.BestSlot(
                    date,
                    -1,
                    -1,
                    count,
                    percentage,
                    participants
            ));
        }

        // 정렬 및 Top3 선택
        List<MeetingDetailData.BestSlot> bestSlots = allSlots.stream()
                .sorted(Comparator
                        .comparingInt(MeetingDetailData.BestSlot::getCount).reversed()
                        .thenComparing(MeetingDetailData.BestSlot::getDate))
                .limit(TOP_N)
                .collect(Collectors.toList());

        return new MeetingDetailData.SummaryData(totalParticipants, bestSlots);
    }

    /**
     * 날짜-슬롯별 참여자 ID Set 생성
     */
    private Map<String, Map<Integer, Set<Long>>> buildDateSlotUsersMap(
            List<MeetingUserSelection> selections
    ) {
        Map<String, Map<Integer, Set<Long>>> result = new HashMap<>();

        for (MeetingUserSelection selection : selections) {
            Long userId = selection.getUserId();
            Map<String, int[]> userSelections = selection.getSelections();

            for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                String date = entry.getKey();
                int[] slots = entry.getValue();

                result.computeIfAbsent(date, k -> new HashMap<>());
                Map<Integer, Set<Long>> slotUsersMap = result.get(date);

                for (int slot : slots) {
                    slotUsersMap.computeIfAbsent(slot, k -> new HashSet<>())
                            .add(userId);
                }
            }
        }

        return result;
    }

    /**
     * 슬롯 범위 (시작~끝)
     */
    private static class SlotRange {
        final int start;
        final int end;

        SlotRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}

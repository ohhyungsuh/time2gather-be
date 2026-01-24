package com.cover.time2gather.util;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingLocation;
import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import com.cover.time2gather.domain.user.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import static com.cover.time2gather.domain.meeting.constants.ReportConstants.*;

/**
 * GPT 레포트 생성을 위한 입력 텍스트 빌더
 */
public class ReportInputTextBuilder {

    private ReportInputTextBuilder() {
    }

    public static String build(Meeting meeting, List<MeetingUserSelection> selections, Map<Long, User> userMap) {
        return build(meeting, selections, userMap, Collections.emptyList(), Collections.emptyList());
    }

    public static String build(
            Meeting meeting,
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap,
            List<MeetingLocation> locations,
            List<MeetingLocationSelection> locationSelections
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(INPUT_MEETING_TITLE).append(meeting.getTitle()).append("\n");

        User host = userMap.get(meeting.getHostUserId());
        String hostName = host != null ? host.getUsername() : UNKNOWN_USER;
        sb.append(INPUT_MEETING_HOST).append(hostName).append("\n");

        // 선택 타입 정보 추가
        sb.append("Selection Type: ").append(meeting.getSelectionType()).append("\n");
        sb.append(INPUT_VOTED_PARTICIPANTS).append(selections.size()).append("\n\n");

        // 확정된 시간/날짜 정보 추가
        sb.append(buildConfirmedTimeInfo(meeting));

        // 장소 투표 정보 추가 (활성화된 경우에만)
        if (Boolean.TRUE.equals(meeting.getLocationVoteEnabled()) && !locations.isEmpty()) {
            sb.append(buildLocationStatistics(meeting, locations, locationSelections, userMap));
        }

        // 날짜별/시간대별 집계 데이터 추가
        if (meeting.getSelectionType() == SelectionType.TIME) {
            sb.append(buildTimeRangeStatistics(selections, userMap, meeting.getIntervalMinutes()));
        } else {
            sb.append(buildDateStatistics(selections, userMap));
        }

        sb.append(INPUT_PARTICIPANT_SELECTIONS);

        for (MeetingUserSelection selection : selections) {
            User user = userMap.get(selection.getUserId());
            String username = user != null ? user.getUsername() : UNKNOWN_USER;
            sb.append("- ").append(username).append(":\n");

            Map<String, int[]> userSelections = selection.getSelections();

            // ALL_DAY 타입 처리
            if (selection.getSelectionType() == SelectionType.ALL_DAY) {
                for (String date : userSelections.keySet()) {
                    String dateWithDayOfWeek = formatDateWithDayOfWeek(date);
                    sb.append("  * ").append(dateWithDayOfWeek).append(": 하루 종일\n");
                }
            } else {
                // TIME 타입 처리 (기존)
                int intervalMinutes = selection.getIntervalMinutes();
                for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                    String date = entry.getKey();
                    String dateWithDayOfWeek = formatDateWithDayOfWeek(date);
                    int[] slots = entry.getValue();

                    String timeSlots = Arrays.stream(slots)
                            .mapToObj(slotIndex -> TimeSlot.fromIndex(slotIndex, intervalMinutes).toTimeString())
                            .collect(Collectors.joining(", "));

                    sb.append("  * ").append(dateWithDayOfWeek).append(": ").append(timeSlots).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 장소 투표 정보 생성
     */
    private static String buildLocationStatistics(
            Meeting meeting,
            List<MeetingLocation> locations,
            List<MeetingLocationSelection> locationSelections,
            Map<Long, User> userMap
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("📍 장소 투표 현황:\n");

        // 확정된 장소가 있는 경우
        Long confirmedLocationId = meeting.getConfirmedLocationId();
        if (confirmedLocationId != null) {
            String confirmedLocationName = locations.stream()
                    .filter(loc -> loc.getId().equals(confirmedLocationId))
                    .map(MeetingLocation::getName)
                    .findFirst()
                    .orElse("알 수 없음");
            sb.append("✅ 확정된 장소: ").append(confirmedLocationName).append("\n\n");
        }

        // 장소별 투표 집계
        Map<Long, Set<Long>> locationVotes = new HashMap<>();
        for (MeetingLocation location : locations) {
            locationVotes.put(location.getId(), new HashSet<>());
        }
        for (MeetingLocationSelection selection : locationSelections) {
            locationVotes.computeIfAbsent(selection.getLocationId(), k -> new HashSet<>())
                    .add(selection.getUserId());
        }

        // 투표 수 내림차순으로 정렬
        List<MeetingLocation> sortedLocations = locations.stream()
                .sorted((loc1, loc2) -> {
                    int votes1 = locationVotes.getOrDefault(loc1.getId(), Collections.emptySet()).size();
                    int votes2 = locationVotes.getOrDefault(loc2.getId(), Collections.emptySet()).size();
                    return Integer.compare(votes2, votes1);
                })
                .toList();

        for (MeetingLocation location : sortedLocations) {
            Set<Long> voterIds = locationVotes.getOrDefault(location.getId(), Collections.emptySet());
            int voteCount = voterIds.size();

            sb.append("- ").append(location.getName()).append(": ").append(voteCount).append("명");

            if (!voterIds.isEmpty()) {
                String voterNames = voterIds.stream()
                        .map(userId -> {
                            User user = userMap.get(userId);
                            return user != null ? user.getUsername() : UNKNOWN_USER;
                        })
                        .collect(Collectors.joining(", "));
                sb.append(" (").append(voterNames).append(")");
            }
            sb.append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * 확정된 시간/날짜 정보 생성
     */
    private static String buildConfirmedTimeInfo(Meeting meeting) {
        LocalDate confirmedDate = meeting.getConfirmedDate();
        if (confirmedDate == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String dateWithDayOfWeek = formatDateWithDayOfWeek(confirmedDate.toString());

        if (meeting.getSelectionType() == SelectionType.ALL_DAY) {
            sb.append("✅ 확정된 날짜: ").append(dateWithDayOfWeek).append("\n\n");
        } else {
            Integer confirmedSlotIndex = meeting.getConfirmedSlotIndex();
            if (confirmedSlotIndex != null) {
                int intervalMinutes = meeting.getIntervalMinutes() != null
                        ? meeting.getIntervalMinutes()
                        : TimeSlot.DEFAULT_INTERVAL_MINUTES;
                String timeStr = TimeSlot.fromIndex(confirmedSlotIndex, intervalMinutes).toTimeString();
                sb.append("✅ 확정된 시간: ").append(dateWithDayOfWeek).append(" ").append(timeStr).append("\n\n");
            } else {
                sb.append("✅ 확정된 날짜: ").append(dateWithDayOfWeek).append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * ALL_DAY 타입용 날짜별 통계 생성
     */
    private static String buildDateStatistics(
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Date Statistics (PRE-CALCULATED - USE THESE EXACT NUMBERS):\n");

        // 날짜별 참여자 집계
        Map<String, Set<String>> dateParticipants = new HashMap<>();

        for (MeetingUserSelection selection : selections) {
            User user = userMap.get(selection.getUserId());
            String username = user != null ? user.getUsername() : UNKNOWN_USER;

            Map<String, int[]> userSelections = selection.getSelections();
            for (String date : userSelections.keySet()) {
                dateParticipants.putIfAbsent(date, new HashSet<>());
                dateParticipants.get(date).add(username);
            }
        }

        // 날짜별로 정렬 (가능 인원 내림차순, 같으면 날짜 오름차순)
        List<Map.Entry<String, Set<String>>> sortedDates = dateParticipants.entrySet().stream()
                .sorted((e1, e2) -> {
                    int countCompare = Integer.compare(e2.getValue().size(), e1.getValue().size());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return e1.getKey().compareTo(e2.getKey());
                })
                .collect(Collectors.toList());

        // 통계 정보 출력
        int totalVoted = selections.size();
        for (Map.Entry<String, Set<String>> entry : sortedDates) {
            String date = entry.getKey();
            Set<String> participants = entry.getValue();
            int availableCount = participants.size();

            String dateWithDayOfWeek = formatDateWithDayOfWeek(date);
            sb.append("- ").append(dateWithDayOfWeek).append(": ");
            sb.append(availableCount).append("명 / ").append(totalVoted).append("명\n");
            sb.append("  * 가능: ").append(String.join(", ", participants)).append("\n");

            // 불가능한 참여자 찾기
            Set<String> notAvailable = findNotAvailableParticipants(selections, userMap, participants);
            if (notAvailable.isEmpty()) {
                sb.append("  * 불가능: -\n");
            } else {
                sb.append("  * 불가능: ").append(String.join(", ", notAvailable)).append("\n");
            }
        }

        sb.append("\n🚨 CRITICAL: Use the EXACT numbers and names from above statistics!\n");
        sb.append("DO NOT recalculate! Just copy the data to your output.\n\n");

        return sb.toString();
    }

    /**
     * TIME 타입용 시간 범위별 통계 생성
     * 연속된 시간 슬롯을 범위로 그룹핑하고 TOP 3를 미리 계산
     */
    private static String buildTimeRangeStatistics(
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap,
            Integer intervalMinutes
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Time Range Statistics (PRE-CALCULATED - USE THESE EXACT DATA FOR TOP 3):\n\n");

        int interval = intervalMinutes != null ? intervalMinutes : TimeSlot.DEFAULT_INTERVAL_MINUTES;
        int totalVoted = selections.size();

        // 모든 시간 범위와 해당 참여자 수집
        List<TimeRangeInfo> allTimeRanges = collectAllTimeRanges(selections, userMap, interval);

        // 가능 인원 내림차순, 같으면 날짜 오름차순, 같으면 시작 시간 오름차순
        allTimeRanges.sort((a, b) -> {
            int countCompare = Integer.compare(b.participants.size(), a.participants.size());
            if (countCompare != 0) return countCompare;
            int dateCompare = a.date.compareTo(b.date);
            if (dateCompare != 0) return dateCompare;
            return Integer.compare(a.startSlot, b.startSlot);
        });

        // TOP 3 출력
        int rank = 1;
        for (TimeRangeInfo range : allTimeRanges) {
            if (rank > 3) break;

            String dateWithDayOfWeek = formatDateWithDayOfWeek(range.date);
            String startTime = TimeSlot.fromIndex(range.startSlot, interval).toTimeString();
            String endTime = TimeSlot.fromIndex(range.endSlot, interval).toTimeString();
            int availableCount = range.participants.size();

            sb.append("### ").append(rank).append("순위\n");
            sb.append("**날짜:** ").append(dateWithDayOfWeek).append(" ").append(startTime).append(" ~ ").append(endTime).append("\n");
            sb.append("**가능 인원:** ").append(availableCount).append("명 / ").append(totalVoted).append("명\n");
            sb.append("- **가능:** ").append(String.join(", ", range.participants)).append("\n");

            Set<String> notAvailable = findNotAvailableParticipants(selections, userMap, range.participants);
            if (notAvailable.isEmpty()) {
                sb.append("- **불가능:** -\n");
            } else {
                sb.append("- **불가능:** ").append(String.join(", ", notAvailable)).append("\n");
            }
            sb.append("\n");
            rank++;
        }

        sb.append("🚨 CRITICAL: Copy the EXACT data above to your '최적 시간대 TOP 3' section!\n");
        sb.append("DO NOT recalculate or re-group time ranges!\n\n");

        return sb.toString();
    }

    /**
     * 모든 시간 범위 수집 (연속된 슬롯을 그룹핑)
     */
    private static List<TimeRangeInfo> collectAllTimeRanges(
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap,
            int intervalMinutes
    ) {
        // 날짜+슬롯별 참여자 집계
        Map<String, Map<Integer, Set<String>>> dateSlotParticipants = new HashMap<>();

        for (MeetingUserSelection selection : selections) {
            User user = userMap.get(selection.getUserId());
            String username = user != null ? user.getUsername() : UNKNOWN_USER;

            for (Map.Entry<String, int[]> entry : selection.getSelections().entrySet()) {
                String date = entry.getKey();
                int[] slots = entry.getValue();

                dateSlotParticipants.putIfAbsent(date, new HashMap<>());
                Map<Integer, Set<String>> slotMap = dateSlotParticipants.get(date);

                for (int slot : slots) {
                    slotMap.putIfAbsent(slot, new HashSet<>());
                    slotMap.get(slot).add(username);
                }
            }
        }

        // 연속된 슬롯을 시간 범위로 그룹핑
        List<TimeRangeInfo> result = new ArrayList<>();

        for (Map.Entry<String, Map<Integer, Set<String>>> dateEntry : dateSlotParticipants.entrySet()) {
            String date = dateEntry.getKey();
            Map<Integer, Set<String>> slotMap = dateEntry.getValue();

            if (slotMap.isEmpty()) continue;

            // 슬롯을 정렬
            List<Integer> sortedSlots = new ArrayList<>(slotMap.keySet());
            Collections.sort(sortedSlots);

            // 연속된 슬롯 중 동일한 참여자를 가진 범위 찾기
            int rangeStart = sortedSlots.get(0);
            Set<String> rangeParticipants = new HashSet<>(slotMap.get(rangeStart));

            for (int i = 1; i <= sortedSlots.size(); i++) {
                boolean isLast = (i == sortedSlots.size());
                boolean isContinuous = !isLast && (sortedSlots.get(i) == sortedSlots.get(i - 1) + 1);
                Set<String> currentParticipants = isLast ? null : slotMap.get(sortedSlots.get(i));
                boolean sameParticipants = !isLast && rangeParticipants.equals(currentParticipants);

                if (isLast || !isContinuous || !sameParticipants) {
                    // 현재 범위 저장
                    int rangeEnd = sortedSlots.get(i - 1);
                    result.add(new TimeRangeInfo(date, rangeStart, rangeEnd, rangeParticipants));

                    // 새 범위 시작
                    if (!isLast) {
                        rangeStart = sortedSlots.get(i);
                        rangeParticipants = new HashSet<>(slotMap.get(rangeStart));
                    }
                }
            }
        }

        return result;
    }

    /**
     * 불가능한 참여자 찾기
     */
    private static Set<String> findNotAvailableParticipants(
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap,
            Set<String> availableParticipants
    ) {
        Set<String> notAvailable = new HashSet<>();
        for (MeetingUserSelection selection : selections) {
            User user = userMap.get(selection.getUserId());
            String username = user != null ? user.getUsername() : UNKNOWN_USER;
            if (!availableParticipants.contains(username)) {
                notAvailable.add(username);
            }
        }
        return notAvailable;
    }

    /**
     * 시간 범위 정보를 담는 내부 클래스
     */
    private static class TimeRangeInfo {
        final String date;
        final int startSlot;
        final int endSlot;
        final Set<String> participants;

        TimeRangeInfo(String date, int startSlot, int endSlot, Set<String> participants) {
            this.date = date;
            this.startSlot = startSlot;
            this.endSlot = endSlot;
            this.participants = participants;
        }
    }

    /**
     * 날짜를 "YYYY-MM-DD (요일)" 형식으로 변환
     * 예: "2025-12-09" -> "2025-12-09 (월)"
     */
    private static String formatDateWithDayOfWeek(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            DayOfWeek dayOfWeek = localDate.getDayOfWeek();
            String koreanDayOfWeek = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN);
            return date + " (" + koreanDayOfWeek + ")";
        } catch (Exception e) {
            // 파싱 실패 시 원본 날짜 반환
            return date;
        }
    }
}

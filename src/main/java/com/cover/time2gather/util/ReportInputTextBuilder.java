package com.cover.time2gather.util;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
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
        StringBuilder sb = new StringBuilder();
        sb.append(INPUT_MEETING_TITLE).append(meeting.getTitle()).append("\n");

        User host = userMap.get(meeting.getHostUserId());
        String hostName = host != null ? host.getUsername() : UNKNOWN_USER;
        sb.append(INPUT_MEETING_HOST).append(hostName).append("\n");

        // 선택 타입 정보 추가
        sb.append("Selection Type: ").append(meeting.getSelectionType()).append("\n");
        sb.append(INPUT_VOTED_PARTICIPANTS).append(selections.size()).append("\n\n");

        // 날짜별 집계 데이터 추가
        sb.append(buildDateStatistics(selections, userMap, meeting.getSelectionType()));

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
                for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                    String date = entry.getKey();
                    String dateWithDayOfWeek = formatDateWithDayOfWeek(date);
                    int[] slots = entry.getValue();

                    String timeSlots = Arrays.stream(slots)
                            .mapToObj(TimeSlotConverter::slotIndexToTimeStr)
                            .collect(Collectors.joining(", "));

                    sb.append("  * ").append(dateWithDayOfWeek).append(": ").append(timeSlots).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 날짜별 참여자 집계 정보 생성
     * GPT가 계산할 필요 없이 바로 사용할 수 있도록 정확한 통계 제공
     */
    private static String buildDateStatistics(
            List<MeetingUserSelection> selections,
            Map<Long, User> userMap,
            SelectionType selectionType
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
            int notAvailableCount = totalVoted - availableCount;

            String dateWithDayOfWeek = formatDateWithDayOfWeek(date);
            sb.append("- ").append(dateWithDayOfWeek).append(": ");
            sb.append(availableCount).append("명 / ").append(totalVoted).append("명");
            if (availableCount == totalVoted) {
                sb.append(" (만장일치)");
            }
            sb.append("\n");
            sb.append("  * 가능: ").append(String.join(", ", participants)).append("\n");

            // 불가능한 참여자 찾기
            Set<String> notAvailable = new HashSet<>();
            for (MeetingUserSelection selection : selections) {
                User user = userMap.get(selection.getUserId());
                String username = user != null ? user.getUsername() : UNKNOWN_USER;
                if (!participants.contains(username)) {
                    notAvailable.add(username);
                }
            }

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

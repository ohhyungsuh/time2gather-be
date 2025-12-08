package com.cover.time2gather.util;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.user.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

package com.cover.time2gather.util;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.user.User;

import java.util.Arrays;
import java.util.List;
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
        sb.append(INPUT_MEETING_DESCRIPTION).append(meeting.getDescription()).append("\n");

        User host = userMap.get(meeting.getHostUserId());
        String hostName = host != null ? host.getUsername() : UNKNOWN_USER;
        sb.append(INPUT_MEETING_HOST).append(hostName).append("\n");

        sb.append(INPUT_VOTED_PARTICIPANTS).append(selections.size()).append("\n\n");
        sb.append(INPUT_PARTICIPANT_SELECTIONS);

        for (MeetingUserSelection selection : selections) {
            User user = userMap.get(selection.getUserId());
            String username = user != null ? user.getUsername() : UNKNOWN_USER;
            sb.append("- ").append(username).append(":\n");

            Map<String, int[]> userSelections = selection.getSelections();
            for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                String date = entry.getKey();
                int[] slots = entry.getValue();

                String timeSlots = Arrays.stream(slots)
                        .mapToObj(TimeSlotConverter::slotIndexToTimeStr)
                        .collect(Collectors.joining(", "));

                sb.append("  * ").append(date).append(": ").append(timeSlots).append("\n");
            }
        }

        return sb.toString();
    }
}

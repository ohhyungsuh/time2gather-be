package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.user.User;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ReportData(
        Meeting meeting,
        List<MeetingUserSelection> selections,
        Map<Long, User> userMap,
        List<MeetingLocation> locations,
        List<MeetingLocationSelection> locationSelections
) {
    /**
     * 하위 호환성을 위한 생성자
     */
    public ReportData(Meeting meeting, List<MeetingUserSelection> selections, Map<Long, User> userMap) {
        this(meeting, selections, userMap, Collections.emptyList(), Collections.emptyList());
    }
}

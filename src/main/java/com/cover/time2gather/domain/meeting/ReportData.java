package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.user.User;

import java.util.List;
import java.util.Map;

public record ReportData(
        Meeting meeting,
        List<MeetingUserSelection> selections,
        Map<Long, User> userMap
) {
}

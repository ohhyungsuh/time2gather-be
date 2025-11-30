package com.cover.time2gather.domain.meeting.client;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.user.User;

import java.util.List;
import java.util.Map;

public interface ReportSummaryClient {

    String generateSummary(Meeting meeting, List<MeetingUserSelection> selections, Map<Long, User> userMap);
}

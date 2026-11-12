package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.meeting.Meeting;

public record AdminParticipatedMeetingItem(
        Long id,
        String meetingCode,
        String title,
        boolean active,
        boolean confirmed
) {
    public static AdminParticipatedMeetingItem from(Meeting meeting) {
        return new AdminParticipatedMeetingItem(meeting.getId(), meeting.getMeetingCode(),
                meeting.getTitle(), Boolean.TRUE.equals(meeting.getIsActive()), meeting.isConfirmed());
    }
}

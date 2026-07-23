package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.SelectionType;

import java.time.LocalDateTime;

public record AdminMeetingItem(
        Long id,
        String meetingCode,
        String title,
        Long hostUserId,
        String hostUsername,
        SelectionType selectionType,
        boolean active,
        boolean confirmed,
        LocalDateTime createdAt
) {
    public static AdminMeetingItem from(Meeting meeting, String hostUsername) {
        return new AdminMeetingItem(
                meeting.getId(),
                meeting.getMeetingCode(),
                meeting.getTitle(),
                meeting.getHostUserId(),
                hostUsername,
                meeting.getSelectionType(),
                Boolean.TRUE.equals(meeting.getIsActive()),
                meeting.isConfirmed(),
                meeting.getCreatedAt()
        );
    }
}

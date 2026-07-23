package com.cover.time2gather.api.admin.dto;

import com.cover.time2gather.domain.admin.AdminMeetingItem;

import java.time.LocalDateTime;

public record AdminMeetingResponse(
        Long id,
        String meetingCode,
        String title,
        Long hostUserId,
        String hostUsername,
        String selectionType,
        boolean active,
        boolean confirmed,
        LocalDateTime createdAt
) {
    public static AdminMeetingResponse from(AdminMeetingItem meeting) {
        return new AdminMeetingResponse(
                meeting.id(),
                meeting.meetingCode(),
                meeting.title(),
                meeting.hostUserId(),
                meeting.hostUsername(),
                meeting.selectionType().name(),
                meeting.active(),
                meeting.confirmed(),
                meeting.createdAt()
        );
    }
}

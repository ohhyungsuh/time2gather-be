package com.cover.time2gather.api.admin.dto;

import com.cover.time2gather.domain.admin.AdminUserItem;
import com.cover.time2gather.domain.admin.AdminParticipatedMeetingItem;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String provider,
        String role,
        LocalDateTime createdAt,
        List<AdminParticipatedMeetingItem> participatedMeetings
) {
    public static AdminUserResponse from(AdminUserItem user) {
        return new AdminUserResponse(
                user.id(),
                user.username(),
                user.email(),
                user.provider().name(),
                user.role().name(),
                user.createdAt(),
                user.participatedMeetings()
        );
    }
}

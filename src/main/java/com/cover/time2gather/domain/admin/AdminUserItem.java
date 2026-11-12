package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserItem(
        Long id,
        String username,
        String email,
        User.AuthProvider provider,
        UserRole role,
        LocalDateTime createdAt,
        List<AdminParticipatedMeetingItem> participatedMeetings
) {
    public static AdminUserItem from(User user) {
        return from(user, List.of());
    }

    public static AdminUserItem from(User user, List<AdminParticipatedMeetingItem> participatedMeetings) {
        return new AdminUserItem(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProvider(),
                user.getRole(),
                user.getCreatedAt(),
                List.copyOf(participatedMeetings)
        );
    }
}

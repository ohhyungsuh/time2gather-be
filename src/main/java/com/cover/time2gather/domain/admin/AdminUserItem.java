package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRole;

import java.time.LocalDateTime;

public record AdminUserItem(
        Long id,
        String username,
        String email,
        User.AuthProvider provider,
        UserRole role,
        LocalDateTime createdAt
) {
    public static AdminUserItem from(User user) {
        return new AdminUserItem(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProvider(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

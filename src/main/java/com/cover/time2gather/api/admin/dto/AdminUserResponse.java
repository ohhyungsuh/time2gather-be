package com.cover.time2gather.api.admin.dto;

import com.cover.time2gather.domain.admin.AdminUserItem;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String provider,
        String role,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(AdminUserItem user) {
        return new AdminUserResponse(
                user.id(),
                user.username(),
                user.email(),
                user.provider().name(),
                user.role().name(),
                user.createdAt()
        );
    }
}

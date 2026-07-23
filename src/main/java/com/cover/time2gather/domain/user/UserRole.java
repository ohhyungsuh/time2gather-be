package com.cover.time2gather.domain.user;

public enum UserRole {
    USER,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}

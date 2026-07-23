package com.cover.time2gather.domain.admin;

public record AdminSummary(
        UserStats users,
        MeetingStats meetings
) {
    public record UserStats(
            long total,
            long registered,
            long anonymous,
            long admins
    ) {
    }

    public record MeetingStats(
            long total,
            long active,
            long confirmed
    ) {
    }
}

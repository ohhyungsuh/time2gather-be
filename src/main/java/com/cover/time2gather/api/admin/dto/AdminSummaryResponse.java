package com.cover.time2gather.api.admin.dto;

import com.cover.time2gather.domain.admin.AdminSummary;

public record AdminSummaryResponse(
        UserStats users,
        MeetingStats meetings
) {
    public static AdminSummaryResponse from(AdminSummary summary) {
        return new AdminSummaryResponse(
                new UserStats(
                        summary.users().total(),
                        summary.users().registered(),
                        summary.users().anonymous(),
                        summary.users().admins()
                ),
                new MeetingStats(
                        summary.meetings().total(),
                        summary.meetings().active(),
                        summary.meetings().confirmed()
                )
        );
    }

    public record UserStats(long total, long registered, long anonymous, long admins) {
    }

    public record MeetingStats(long total, long active, long confirmed) {
    }
}

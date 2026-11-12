package com.cover.time2gather.domain.admin;

import java.time.Instant;

/** Current stored state, not historical event totals or a time-bounded cohort. */
public record AdminUsageStats(
        Instant generatedAt,
        String basis,
        int activationResponseThreshold,
        long totalMeetings,
        long activeMeetings,
        long respondedMeetings,
        long activatedMeetings,
        long confirmedMeetings,
        long externalResponses,
        long hosts,
        long repeatHosts,
        double activationRate,
        double confirmationRate,
        double repeatHostRate
) { }

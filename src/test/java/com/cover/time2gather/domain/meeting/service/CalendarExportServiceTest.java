package com.cover.time2gather.domain.meeting.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarExportServiceTest {

    private final CalendarExportService calendarExportService = new CalendarExportService();

    @Test
    void allDayEventUsesDateValuesInsteadOfMidnightDateTimes() {
        String ics = new String(calendarExportService.createIcsFile(
                "종일 약속",
                "설명",
                "2026-10-24",
                "ALL_DAY",
                "Asia/Seoul",
                30
        ), StandardCharsets.UTF_8);

        assertThat(ics)
                .contains("DTSTART;VALUE=DATE:20261024")
                .contains("DTEND;VALUE=DATE:20261025")
                .doesNotContain("DTSTART;TZID=Asia/Seoul:20261024T000000");
    }

    @Test
    void timedEventResolvesMeetingTimezoneAndIntervalToUtc() {
        String ics = new String(calendarExportService.createIcsFile(
                "저녁 약속",
                null,
                "2026-10-24",
                "18:30",
                "Asia/Seoul",
                30
        ), StandardCharsets.UTF_8);

        assertThat(ics)
                .contains("DTSTART:20261024T093000Z")
                .contains("DTEND:20261024T100000Z");
    }

    @Test
    void exactConfirmedTimeDoesNotNeedToAlignWithVotingInterval() {
        String ics = new String(calendarExportService.createIcsFile(
                "정확한 시간 약속",
                null,
                "2026-10-24",
                "18:30",
                "Asia/Seoul",
                60
        ), StandardCharsets.UTF_8);

        assertThat(ics)
                .contains("DTSTART:20261024T093000Z")
                .contains("DTEND:20261024T103000Z");
    }

    @Test
    void exportedInstantsFollowSeasonalOffsetsAndCrossDateBoundaries() {
        String winter = new String(calendarExportService.createIcsFile("Winter", null, "2026-01-15", "12:00", "America/New_York", 60), StandardCharsets.UTF_8);
        String summer = new String(calendarExportService.createIcsFile("Summer", null, "2026-07-15", "12:00", "America/New_York", 60), StandardCharsets.UTF_8);
        String fractional = new String(calendarExportService.createIcsFile("Midnight", null, "2026-01-01", "00:15", "Asia/Kathmandu", 30), StandardCharsets.UTF_8);
        assertThat(winter).contains("DTSTART:20260115T170000Z");
        assertThat(summer).contains("DTSTART:20260715T160000Z");
        assertThat(fractional).contains("DTSTART:20251231T183000Z", "DTEND:20251231T190000Z");
    }
}

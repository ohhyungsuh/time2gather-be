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
    void timedEventKeepsMeetingTimezoneAndInterval() {
        String ics = new String(calendarExportService.createIcsFile(
                "저녁 약속",
                null,
                "2026-10-24",
                "18:30",
                "Asia/Seoul",
                30
        ), StandardCharsets.UTF_8);

        assertThat(ics)
                .contains("DTSTART;TZID=Asia/Seoul:20261024T183000")
                .contains("DTEND;TZID=Asia/Seoul:20261024T190000");
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
                .contains("DTSTART;TZID=Asia/Seoul:20261024T183000")
                .contains("DTEND;TZID=Asia/Seoul:20261024T193000");
    }
}

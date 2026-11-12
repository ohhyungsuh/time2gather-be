package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class MeetingTimeZoneTest {
    @Test void validatesIanaZones() {
        assertThat(MeetingTimeZone.zone("Asia/Kathmandu").getId()).isEqualTo("Asia/Kathmandu");
        assertThatThrownBy(() -> MeetingTimeZone.zone("Mars/Olympus"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEETING_TIMEZONE_INVALID));
    }
    @Test void resolvesHalfHourAndQuarterHourZonesAcrossDateBoundaries() {
        assertThat(MeetingTimeZone.resolve(LocalDate.parse("2026-01-01"), LocalTime.parse("00:15"), "Asia/Kathmandu").toInstant())
                .isEqualTo(Instant.parse("2025-12-31T18:30:00Z"));
        assertThat(MeetingTimeZone.resolve(LocalDate.parse("2026-01-01"), LocalTime.parse("00:15"), "Asia/Kolkata").toInstant())
                .isEqualTo(Instant.parse("2025-12-31T18:45:00Z"));
    }
    @Test void appliesOffsetsForTheMeetingDate() {
        assertThat(MeetingTimeZone.resolve(LocalDate.parse("2026-01-15"), LocalTime.NOON, "America/New_York").toInstant())
                .isEqualTo(Instant.parse("2026-01-15T17:00:00Z"));
        assertThat(MeetingTimeZone.resolve(LocalDate.parse("2026-07-15"), LocalTime.NOON, "America/New_York").toInstant())
                .isEqualTo(Instant.parse("2026-07-15T16:00:00Z"));
    }
    @Test void rejectsNonexistentAndAmbiguousWallTimes() {
        assertThatThrownBy(() -> MeetingTimeZone.resolve(LocalDate.parse("2026-03-08"), LocalTime.parse("02:30"), "America/New_York"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEETING_TIME_NONEXISTENT));
        assertThatThrownBy(() -> MeetingTimeZone.resolve(LocalDate.parse("2026-11-01"), LocalTime.parse("01:30"), "America/New_York"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEETING_TIME_AMBIGUOUS));
    }
    @Test void newMeetingRejectsAmbiguousSlotsButDateOnlyKeepsCalendarDates() {
        assertThatThrownBy(() -> Meeting.create("code", "name", null, 1L, "America/New_York", SelectionType.TIME, 30, Map.of("2026-11-01", new int[]{3})))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEETING_TIME_AMBIGUOUS));
        var dateOnly = Meeting.create("code", "name", null, 1L, "America/New_York", SelectionType.ALL_DAY, 30, Map.of("2026-11-01", new int[0]));
        dateOnly.confirm(LocalDate.parse("2026-11-01"), null);
        assertThat(dateOnly.getConfirmedDate()).isEqualTo(LocalDate.parse("2026-11-01"));
    }
    @Test void existingApiCallersKeepTheirLegacyDefaultZone() {
        var meeting = Meeting.create("code", "name", null, 1L, null, SelectionType.ALL_DAY, 60, Map.of("2026-01-01", new int[0]));
        assertThat(meeting.getTimezone()).isEqualTo("Asia/Seoul");
    }
}

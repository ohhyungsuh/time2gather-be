package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Date + slot keys are wall times in this IANA zone, never in the viewer's zone. */
public final class MeetingTimeZone {
    private MeetingTimeZone() {}

    public static ZoneId zone(String value) {
        if (value == null || !ZoneId.getAvailableZoneIds().contains(value)) {
            throw new BusinessException(ErrorCode.MEETING_TIMEZONE_INVALID);
        }
        return ZoneId.of(value);
    }

    public static ZonedDateTime resolve(LocalDate date, LocalTime time, String timezone) {
        ZoneId zone = zone(timezone);
        var local = date.atTime(time);
        var offsets = zone.getRules().getValidOffsets(local);
        if (offsets.isEmpty()) throw new BusinessException(ErrorCode.MEETING_TIME_NONEXISTENT);
        if (offsets.size() > 1) throw new BusinessException(ErrorCode.MEETING_TIME_AMBIGUOUS);
        return ZonedDateTime.ofStrict(local, offsets.getFirst(), zone);
    }
}

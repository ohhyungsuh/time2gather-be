package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * iCalendar(ICS) 파일 생성 서비스
 * Google Calendar, iOS Calendar 등 표준 캘린더 앱에서 import 가능
 */
@Slf4j
@Service
public class CalendarExportService {

    private static final String PROD_ID = "-//time2gather//Meeting Scheduler 1.0//EN";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final UidGenerator uidGenerator = new RandomUidGenerator();

    /**
     * 선택한 날짜/시간대를 ICS 파일로 변환
     *
     * @param meetingTitle 모임 제목
     * @param meetingDescription 모임 설명
     * @param dateStr 날짜 (yyyy-MM-dd)
     * @param timeSlot 시간 슬롯 (HH:mm 형식 또는 "ALL_DAY")
     * @param timezone 타임존 (예: Asia/Seoul)
     * @param intervalMinutes 시간 간격 (분 단위)
     * @return ICS 파일 바이트 배열
     */
    public byte[] createIcsFile(
            String meetingTitle,
            String meetingDescription,
            String dateStr,
            String timeSlot,
            String timezone,
            int intervalMinutes
    ) {
        try {
            // Calendar 생성
            Calendar calendar = new Calendar();
            calendar.getProperties().add(new ProdId(PROD_ID));
            calendar.getProperties().add(new Version());
            calendar.getProperties().add(new CalScale("GREGORIAN"));

            // Event 생성
            VEvent event = createEvent(meetingTitle, meetingDescription, dateStr, timeSlot, timezone, intervalMinutes);
            calendar.getComponents().add(event);

            // ICS 파일 생성
            return outputCalendar(calendar);

        } catch (Exception e) {
            log.error("Failed to create ICS file", e);
            throw new IllegalStateException("캘린더 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    private VEvent createEvent(
            String title,
            String description,
            String dateStr,
            String timeSlotStr,
            String timezoneStr,
            int intervalMinutes
    ) {
        // 날짜 파싱
        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
        ZoneId zoneId = ZoneId.of(timezoneStr);

        // VEvent 생성
        VEvent event = new VEvent();
        event.getProperties().add(new Summary(title));

        // ALL_DAY 처리
        if ("ALL_DAY".equals(timeSlotStr)) {
            // 종일 일정으로 설정
            ZonedDateTime startDateTime = date.atStartOfDay(zoneId);
            ZonedDateTime endDateTime = date.plusDays(1).atStartOfDay(zoneId);

            event.getProperties().add(new DtStart<>(startDateTime));
            event.getProperties().add(new DtEnd<>(endDateTime));
        } else {
            // 특정 시간대 처리
            TimeSlot timeSlot = TimeSlot.fromTimeString(timeSlotStr);
            ZonedDateTime startDateTime = date.atTime(timeSlot.getHour(), timeSlot.getMinute()).atZone(zoneId);
            ZonedDateTime endDateTime = startDateTime.plusMinutes(intervalMinutes);

            event.getProperties().add(new DtStart<>(startDateTime));
            event.getProperties().add(new DtEnd<>(endDateTime));
        }

        // UID 추가 (필수)
        event.getProperties().add(uidGenerator.generateUid());

        // 설명 추가
        if (description != null && !description.isBlank()) {
            event.getProperties().add(new Description(description));
        }

        // 생성 시간 (현재 시간을 Instant로)
        Instant now = Instant.now();
        event.getProperties().add(new Created(now));
        event.getProperties().add(new LastModified(now));
        event.getProperties().add(new DtStamp(now));

        // Status
        event.getProperties().add(new Status("CONFIRMED"));

        return event;
    }

    private byte[] outputCalendar(Calendar calendar) throws IOException {
        CalendarOutputter outputter = new CalendarOutputter();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            outputter.output(calendar, out);
            return out.toByteArray();
        }
    }
}


package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.PropertyList;
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
import java.util.List;

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
            // Event 생성
            VEvent event = createEvent(meetingTitle, meetingDescription, dateStr, timeSlot, timezone, intervalMinutes);

            // PropertyList 생성 (raw type 사용)
            PropertyList properties = new PropertyList();
            properties.add(new ProdId(PROD_ID));
            properties.add(new Version());
            properties.add(new CalScale("GREGORIAN"));

            // ComponentList 생성 및 이벤트 추가
            ComponentList<VEvent> components = new ComponentList<>(List.of(event));

            // Calendar 생성
            Calendar calendar = new Calendar(properties, components);

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

        // PropertyList 생성
        PropertyList properties = new PropertyList();
        properties.add(new Summary(title));

        // 시작/종료 시간 설정
        if ("ALL_DAY".equals(timeSlotStr)) {
            // 종일 일정으로 설정
            ZonedDateTime startDateTime = date.atStartOfDay(zoneId);
            ZonedDateTime endDateTime = date.plusDays(1).atStartOfDay(zoneId);

            properties.add(new DtStart<>(startDateTime));
            properties.add(new DtEnd<>(endDateTime));
        } else {
            // 특정 시간대 처리
            TimeSlot timeSlot = TimeSlot.fromTimeString(timeSlotStr, intervalMinutes);
            ZonedDateTime startDateTime = date.atTime(timeSlot.getHour(), timeSlot.getMinute()).atZone(zoneId);
            ZonedDateTime endDateTime = startDateTime.plusMinutes(intervalMinutes);

            properties.add(new DtStart<>(startDateTime));
            properties.add(new DtEnd<>(endDateTime));
        }

        // UID 추가 (필수)
        properties.add(uidGenerator.generateUid());

        // 설명 추가
        if (description != null && !description.isBlank()) {
            properties.add(new Description(description));
        }

        // 생성 시간 (현재 시간을 Instant로)
        Instant now = Instant.now();
        properties.add(new Created(now));
        properties.add(new LastModified(now));
        properties.add(new DtStamp(now));

        // Status
        properties.add(new Status("CONFIRMED"));

        // VEvent 생성 (PropertyList를 생성자에 전달)
        return new VEvent(properties);
    }

    private byte[] outputCalendar(Calendar calendar) throws IOException {
        CalendarOutputter outputter = new CalendarOutputter();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            outputter.output(calendar, out);
            return out.toByteArray();
        }
    }
}


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
            // 날짜 파싱
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            ZoneId zoneId = ZoneId.of(timezone);

            // 시작/종료 시간 계산
            ZonedDateTime startDateTime;
            ZonedDateTime endDateTime;

            if ("ALL_DAY".equals(timeSlot)) {
                // 종일 일정
                startDateTime = date.atStartOfDay(zoneId);
                endDateTime = date.plusDays(1).atStartOfDay(zoneId);
            } else {
                // 특정 시간대
                TimeSlot ts = TimeSlot.fromTimeString(timeSlot, intervalMinutes);
                startDateTime = date.atTime(ts.getHour(), ts.getMinute()).atZone(zoneId);
                endDateTime = startDateTime.plusMinutes(intervalMinutes);
            }

            // Event PropertyList 생성 (VEvent 생성 전에)
            PropertyList eventProperties = new PropertyList();
            eventProperties.add(new DtStart<>(startDateTime));
            eventProperties.add(new DtEnd<>(endDateTime));
            eventProperties.add(new Summary(meetingTitle));
            eventProperties.add(uidGenerator.generateUid());
            eventProperties.add(new DtStamp(Instant.now()));
            eventProperties.add(new Created(Instant.now()));
            eventProperties.add(new LastModified(Instant.now()));
            eventProperties.add(new Status("CONFIRMED"));

            if (meetingDescription != null && !meetingDescription.isBlank()) {
                eventProperties.add(new Description(meetingDescription));
            }

            // VEvent 생성 (PropertyList를 포함하여)
            VEvent event = new VEvent(eventProperties);

            // Calendar PropertyList 생성
            PropertyList calendarProperties = new PropertyList();
            calendarProperties.add(new ProdId(PROD_ID));
            calendarProperties.add(new Version(Version.VALUE_2_0, Version.VALUE_2_0));
            calendarProperties.add(new CalScale("GREGORIAN"));

            // ComponentList 생성
            ComponentList<VEvent> components = new ComponentList<>();
            components.add(event);

            // Calendar 생성
            Calendar calendar = new Calendar(calendarProperties, components);

            // ICS 파일 생성
            return outputCalendar(calendar);

        } catch (Exception e) {
            log.error("Failed to create ICS file", e);
            throw new IllegalStateException("캘린더 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    private byte[] outputCalendar(Calendar calendar) throws IOException {
        CalendarOutputter outputter = new CalendarOutputter();
        // iOS 호환성을 위해 validation 비활성화
        outputter.setValidating(false);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            outputter.output(calendar, out);
            return out.toByteArray();
        }
    }
}


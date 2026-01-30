package com.cover.time2gather.domain.meeting.vo;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 시간 슬롯을 나타내는 값 객체
 * 슬롯 간격(intervalMinutes)에 따라 하루의 슬롯 개수가 결정됨
 * 기본값: 60분 간격 (하루 24개 슬롯, 0~23)
 * - 예: 60분 간격 -> 0: 00:00, 1: 01:00, ..., 23: 23:00
 * - 예: 30분 간격 -> 0: 00:00, 1: 00:30, ..., 47: 23:30
 * - 예: 15분 간격 -> 0: 00:00, 1: 00:15, ..., 95: 23:45
 */
@Getter
@EqualsAndHashCode
public class TimeSlot {

    /**
     * 기본 슬롯 간격 (분)
     */
    public static final int DEFAULT_INTERVAL_MINUTES = 60;

    private static final int MINUTES_PER_DAY = 24 * 60;

    private final int slotIndex;
    private final int intervalMinutes;

    // 캐시된 값들
    private final int slotsPerDay;
    private final int minSlotIndex;
    private final int maxSlotIndex;

    private TimeSlot(int slotIndex, int intervalMinutes) {
        validateIntervalMinutes(intervalMinutes);
        this.intervalMinutes = intervalMinutes;
        this.slotsPerDay = MINUTES_PER_DAY / intervalMinutes;
        this.minSlotIndex = 0;
        this.maxSlotIndex = slotsPerDay - 1;

        validateSlotIndex(slotIndex);
        this.slotIndex = slotIndex;
    }

    /**
     * 기본 간격(30분)으로 슬롯 인덱스에서 TimeSlot 생성
     * @param slotIndex 슬롯 인덱스
     * @return TimeSlot 객체
     */
    public static TimeSlot fromIndex(int slotIndex) {
        return fromIndex(slotIndex, DEFAULT_INTERVAL_MINUTES);
    }

    /**
     * 지정된 간격으로 슬롯 인덱스에서 TimeSlot 생성
     * @param slotIndex 슬롯 인덱스
     * @param intervalMinutes 슬롯 간격 (분)
     * @return TimeSlot 객체
     */
    public static TimeSlot fromIndex(int slotIndex, int intervalMinutes) {
        return new TimeSlot(slotIndex, intervalMinutes);
    }

    /**
     * 기본 간격(30분)으로 "HH:mm" 문자열에서 TimeSlot 생성
     * @param timeStr "09:00", "09:30" 형식
     * @return TimeSlot 객체
     */
    public static TimeSlot fromTimeString(String timeStr) {
        return fromTimeString(timeStr, DEFAULT_INTERVAL_MINUTES);
    }

    /**
     * 지정된 간격으로 "HH:mm" 문자열에서 TimeSlot 생성
     * @param timeStr "HH:mm" 형식
     * @param intervalMinutes 슬롯 간격 (분)
     * @return TimeSlot 객체
     */
    public static TimeSlot fromTimeString(String timeStr, int intervalMinutes) {
        String[] parts = timeStr.split(":");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.TIMESLOT_INVALID_FORMAT);
        }

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        if (hour < 0 || hour > 23) {
            throw new BusinessException(ErrorCode.TIMESLOT_INVALID_HOUR);
        }
        if (minute < 0 || minute > 59) {
            throw new BusinessException(ErrorCode.TIMESLOT_INVALID_MINUTE);
        }

        int totalMinutes = hour * 60 + minute;
        if (totalMinutes % intervalMinutes != 0) {
            throw new BusinessException(ErrorCode.TIMESLOT_NOT_ALIGNED, intervalMinutes);
        }

        int slotIndex = totalMinutes / intervalMinutes;
        return new TimeSlot(slotIndex, intervalMinutes);
    }

    /**
     * "HH:mm" 형식 문자열로 변환
     * @return "09:00", "09:30" 형식
     */
    public String toTimeString() {
        int totalMinutes = slotIndex * intervalMinutes;
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;

        return String.format("%02d:%02d", hour, minute);
    }

    /**
     * 시간 계산
     * @return 시 (0~23)
     */
    public int getHour() {
        return (slotIndex * intervalMinutes) / 60;
    }

    /**
     * 분 계산
     * @return 분 (0~59)
     */
    public int getMinute() {
        return (slotIndex * intervalMinutes) % 60;
    }

    /**
     * 다음 슬롯인지 검증
     * @param other 비교 대상 슬롯
     * @return 다음 슬롯이면 true
     */
    public boolean isNextSlot(TimeSlot other) {
        return this.slotIndex + 1 == other.slotIndex;
    }

    /**
     * 특정 시간대 이후인지 확인
     * @param other 비교 대상 슬롯
     * @return 이후면 true
     */
    public boolean isAfter(TimeSlot other) {
        return this.slotIndex > other.slotIndex;
    }

    /**
     * 특정 시간대 이전인지 확인
     * @param other 비교 대상 슬롯
     * @return 이전이면 true
     */
    public boolean isBefore(TimeSlot other) {
        return this.slotIndex < other.slotIndex;
    }

    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < minSlotIndex || slotIndex > maxSlotIndex) {
            throw new BusinessException(ErrorCode.TIMESLOT_INDEX_OUT_OF_RANGE, 
                    intervalMinutes, minSlotIndex, maxSlotIndex, slotIndex);
        }
    }

    private void validateIntervalMinutes(int intervalMinutes) {
        if (intervalMinutes <= 0) {
            throw new BusinessException(ErrorCode.TIMESLOT_INTERVAL_POSITIVE);
        }
        if (MINUTES_PER_DAY % intervalMinutes != 0) {
            throw new BusinessException(ErrorCode.TIMESLOT_INTERVAL_DIVISOR, MINUTES_PER_DAY);
        }
    }

    @Override
    public String toString() {
        return toTimeString();
    }
}


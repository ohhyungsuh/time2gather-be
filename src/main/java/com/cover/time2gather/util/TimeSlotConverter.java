package com.cover.time2gather.util;

/**
 * 시간을 30분 단위 slotIndex(0~47)와 "HH:mm" 문자열 간 변환하는 유틸리티
 */
public class TimeSlotConverter {

    /**
     * "HH:mm" 문자열을 slotIndex로 변환
     * @param time "HH:mm" 형식 (예: "09:00", "09:30")
     * @return slotIndex (0~47)
     */
    public static int timeStrToSlotIndex(String time) {
        String[] parts = time.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid time format. Expected HH:mm");
        }

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23");
        }
        if (minute != 0 && minute != 30) {
            throw new IllegalArgumentException("Minute must be 0 or 30");
        }

        return hour * 2 + (minute == 30 ? 1 : 0);
    }

    /**
     * slotIndex를 "HH:mm" 문자열로 변환
     * @param slotIndex 0~47
     * @return "HH:mm" 형식 문자열
     */
    public static String slotIndexToTimeStr(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 47) {
            throw new IllegalArgumentException("SlotIndex must be between 0 and 47");
        }

        int totalMinutes = slotIndex * 30;
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;

        return String.format("%02d:%02d", hour, minute);
    }
}


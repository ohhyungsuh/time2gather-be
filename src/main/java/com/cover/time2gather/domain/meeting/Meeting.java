package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.common.BaseEntity;
import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "meetings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_code", unique = true, nullable = false, length = 100)
    private String meetingCode;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    @Column(length = 50)
    private String timezone = "Asia/Seoul";

    /**
     * 선택 타입 (TIME: 시간 단위, ALL_DAY: 일 단위)
     * 기본값: TIME
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "selection_type", nullable = false, length = 20)
    private SelectionType selectionType = SelectionType.TIME;

    /**
     * 시간 슬롯 간격 (분)
     * 기본값: 60분
     * ALL_DAY 타입인 경우에도 저장하지만 실제로는 사용되지 않음
     */
    @Column(name = "interval_minutes", nullable = false)
    private Integer intervalMinutes = TimeSlot.DEFAULT_INTERVAL_MINUTES;

    /**
     * 날짜별 가능한 시간대 (slotIndex 배열)
     * TIME 타입: {"2024-02-15": [18, 19, 20, 21], "2024-02-16": [22, 23, 24]}
     * ALL_DAY 타입: {"2024-02-15": [], "2024-02-16": []} (빈 배열 = 하루 종일)
     */
    @Type(JsonType.class)
    @Column(name = "available_dates", columnDefinition = "json", nullable = false)
    private Map<String, int[]> availableDates;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "confirmed_date")
    private LocalDate confirmedDate;

    @Column(name = "confirmed_slot_index")
    private Integer confirmedSlotIndex;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    public static Meeting create(
            String meetingCode,
            String title,
            String description,
            Long hostUserId,
            String timezone,
            SelectionType selectionType,
            Integer intervalMinutes,
            Map<String, int[]> availableDates
    ) {
        Meeting meeting = new Meeting();
        meeting.meetingCode = meetingCode;
        meeting.title = title;
        meeting.description = description;
        meeting.hostUserId = hostUserId;
        meeting.timezone = timezone != null ? timezone : "Asia/Seoul";
        meeting.selectionType = selectionType != null ? selectionType : SelectionType.TIME;
        meeting.intervalMinutes = intervalMinutes != null ? intervalMinutes : TimeSlot.DEFAULT_INTERVAL_MINUTES;
        meeting.availableDates = availableDates;
        meeting.isActive = true;

        // TimeSlot 검증 (타입에 따라)
        if (meeting.selectionType == SelectionType.TIME) {
            meeting.validateTimeSlots();
        } else {
            meeting.validateAllDayDates();
        }

        return meeting;
    }

    /**
     * TimeSlot 유효성 검증
     * 도메인 규칙: 모든 슬롯 인덱스는 해당 간격에 맞는 범위여야 함
     */
    private void validateTimeSlots() {
        if (availableDates == null || availableDates.isEmpty()) {
            throw new IllegalArgumentException("최소 하나의 날짜와 시간대를 선택해야 합니다.");
        }

        for (Map.Entry<String, int[]> entry : availableDates.entrySet()) {
            int[] slots = entry.getValue();
            if (slots == null || slots.length == 0) {
                throw new IllegalArgumentException("각 날짜마다 최소 하나의 시간대를 선택해야 합니다.");
            }

            for (int slotIndex : slots) {
                // TimeSlot 생성으로 검증 (범위 체크)
                TimeSlot.fromIndex(slotIndex, intervalMinutes);
            }
        }
    }

    /**
     * ALL_DAY 타입 날짜 유효성 검증
     * 도메인 규칙: 최소 하나의 날짜가 있어야 하며, 각 날짜는 빈 배열이어야 함
     */
    private void validateAllDayDates() {
        if (availableDates == null || availableDates.isEmpty()) {
            throw new IllegalArgumentException("최소 하나의 날짜를 선택해야 합니다.");
        }

        for (Map.Entry<String, int[]> entry : availableDates.entrySet()) {
            int[] slots = entry.getValue();
            // ALL_DAY 타입은 빈 배열이어야 함
            if (slots != null && slots.length > 0) {
                throw new IllegalArgumentException("일 단위 선택(ALL_DAY)인 경우 시간대는 빈 배열이어야 합니다.");
            }
        }
    }

    public boolean isConfirmed() {
        return confirmedDate != null;
    }

    public void confirm(LocalDate date, Integer slotIndex) {
        if (isConfirmed()) {
            throw new IllegalStateException("이미 확정된 미팅입니다. 먼저 확정을 취소해주세요.");
        }

        String dateString = date.toString();
        if (!availableDates.containsKey(dateString)) {
            throw new IllegalArgumentException("유효하지 않은 날짜입니다: " + dateString);
        }

        if (selectionType == SelectionType.TIME) {
            validateSlotIndex(dateString, slotIndex);
        } else {
            // ALL_DAY 타입인 경우 slotIndex는 무시되거나 null이어야 함
            slotIndex = null;
        }

        this.confirmedDate = date;
        this.confirmedSlotIndex = slotIndex;
        this.confirmedAt = LocalDateTime.now();
    }

    private void validateSlotIndex(String dateString, Integer slotIndex) {
        if (slotIndex == null) {
            throw new IllegalArgumentException("TIME 타입 미팅은 slotIndex가 필요합니다.");
        }

        int[] availableSlots = availableDates.get(dateString);
        boolean slotExists = false;
        for (int slot : availableSlots) {
            if (slot == slotIndex) {
                slotExists = true;
                break;
            }
        }

        if (!slotExists) {
            throw new IllegalArgumentException("유효하지 않은 슬롯 인덱스입니다: " + slotIndex);
        }
    }

    public void cancelConfirmation() {
        if (!isConfirmed()) {
            throw new IllegalStateException("확정되지 않은 미팅입니다.");
        }

        this.confirmedDate = null;
        this.confirmedSlotIndex = null;
        this.confirmedAt = null;
    }
}

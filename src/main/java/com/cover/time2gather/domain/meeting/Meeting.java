package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.common.BaseEntity;
import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
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

    // 장소 투표 관련 필드
    @Column(name = "location_vote_enabled", nullable = false)
    private Boolean locationVoteEnabled = false;

    @Column(name = "confirmed_location_id")
    private Long confirmedLocationId;

    @Column(name = "location_confirmed_at")
    private LocalDateTime locationConfirmedAt;

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
        return create(meetingCode, title, description, hostUserId, timezone,
                selectionType, intervalMinutes, availableDates, false);
    }

    public static Meeting create(
            String meetingCode,
            String title,
            String description,
            Long hostUserId,
            String timezone,
            SelectionType selectionType,
            Integer intervalMinutes,
            Map<String, int[]> availableDates,
            Boolean locationVoteEnabled
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
        meeting.locationVoteEnabled = locationVoteEnabled != null ? locationVoteEnabled : false;

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
            throw new BusinessException(ErrorCode.MEETING_TIME_REQUIRED);
        }

        for (Map.Entry<String, int[]> entry : availableDates.entrySet()) {
            int[] slots = entry.getValue();
            if (slots == null || slots.length == 0) {
                throw new BusinessException(ErrorCode.MEETING_DATE_SLOT_REQUIRED);
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
            throw new BusinessException(ErrorCode.MEETING_DATE_REQUIRED);
        }

        for (Map.Entry<String, int[]> entry : availableDates.entrySet()) {
            int[] slots = entry.getValue();
            // ALL_DAY 타입은 빈 배열이어야 함
            if (slots != null && slots.length > 0) {
                throw new BusinessException(ErrorCode.MEETING_ALL_DAY_EMPTY_SLOTS);
            }
        }
    }

    public boolean isConfirmed() {
        return confirmedDate != null;
    }

    public void confirm(LocalDate date, Integer slotIndex) {
        if (isConfirmed()) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_CONFIRMED);
        }

        String dateString = date.toString();
        if (!availableDates.containsKey(dateString)) {
            throw new BusinessException(ErrorCode.MEETING_INVALID_DATE, dateString);
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
            throw new BusinessException(ErrorCode.MEETING_SLOT_INDEX_REQUIRED);
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
            throw new BusinessException(ErrorCode.MEETING_SLOT_INDEX_INVALID, slotIndex);
        }
    }

    public void cancelConfirmation() {
        if (!isConfirmed()) {
            throw new BusinessException(ErrorCode.MEETING_NOT_CONFIRMED);
        }

        this.confirmedDate = null;
        this.confirmedSlotIndex = null;
        this.confirmedAt = null;
    }

    // 장소 투표 관련 메서드

    public boolean isLocationVoteEnabled() {
        return Boolean.TRUE.equals(locationVoteEnabled);
    }

    public boolean isLocationConfirmed() {
        return confirmedLocationId != null;
    }

    public void confirmLocation(Long locationId) {
        if (!isLocationVoteEnabled()) {
            throw new BusinessException(ErrorCode.LOCATION_VOTE_NOT_ENABLED);
        }
        if (isLocationConfirmed()) {
            throw new BusinessException(ErrorCode.LOCATION_ALREADY_CONFIRMED);
        }
        if (locationId == null) {
            throw new BusinessException(ErrorCode.LOCATION_ID_REQUIRED);
        }

        this.confirmedLocationId = locationId;
        this.locationConfirmedAt = LocalDateTime.now();
    }

    public void cancelLocationConfirmation() {
        if (!isLocationConfirmed()) {
            throw new BusinessException(ErrorCode.LOCATION_NOT_CONFIRMED);
        }

        this.confirmedLocationId = null;
        this.locationConfirmedAt = null;
    }

    public void enableLocationVote() {
        this.locationVoteEnabled = true;
    }

    public void disableLocationVote() {
        if (isLocationConfirmed()) {
            throw new BusinessException(ErrorCode.LOCATION_CANNOT_DISABLE);
        }
        this.locationVoteEnabled = false;
    }
}

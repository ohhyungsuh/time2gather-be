package com.cover.time2gather.domain.meeting;

import static com.cover.time2gather.domain.meeting.vo.TimeSlot.*;

import com.cover.time2gather.domain.common.BaseEntity;
import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.Arrays;
import java.util.Map;

@Entity
@Table(
        name = "meeting_user_selections",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_user_meeting",
                columnNames = {"meeting_id", "user_id"}
        ),
        indexes = @Index(name = "idx_meeting_id", columnList = "meeting_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingUserSelection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 선택 타입 (TIME: 시간 단위, ALL_DAY: 일 단위)
     * Meeting의 selectionType과 동일해야 함
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "selection_type", nullable = false, length = 20)
    private SelectionType selectionType = SelectionType.TIME;

    /**
     * 시간 슬롯 간격 (분)
     * Meeting의 intervalMinutes와 동일해야 함
     */
    @Column(name = "interval_minutes", nullable = false)
    private Integer intervalMinutes = DEFAULT_INTERVAL_MINUTES;

    /**
     * 날짜별 선택한 시간대 (slotIndex 배열)
     * TIME 타입: {"2024-02-15": [18, 19, 21], "2024-02-16": [22, 23]}
     * ALL_DAY 타입: {"2024-02-15": [], "2024-02-16": []} (빈 배열 = 하루 종일)
     */
    @Type(JsonType.class)
    @Column(name = "selections", columnDefinition = "json", nullable = false)
    private Map<String, int[]> selections;

    /**
     * 기본 간격(60분)으로 선택 생성
     */
    public static MeetingUserSelection create(Long meetingId, Long userId, Map<String, int[]> selections) {
        return create(meetingId, userId, SelectionType.TIME, DEFAULT_INTERVAL_MINUTES, selections);
    }

    /**
     * 지정된 타입과 간격으로 선택 생성
     */
    public static MeetingUserSelection create(Long meetingId, Long userId, SelectionType selectionType, Integer intervalMinutes, Map<String, int[]> selections) {
        MeetingUserSelection selection = new MeetingUserSelection();
        selection.meetingId = meetingId;
        selection.userId = userId;
        selection.selectionType = selectionType != null ? selectionType : SelectionType.TIME;
        selection.intervalMinutes = intervalMinutes != null ? intervalMinutes : DEFAULT_INTERVAL_MINUTES;
        selection.selections = selections;

        // TimeSlot 검증 (TIME 타입인 경우만)
        if (selection.selectionType == SelectionType.TIME) {
            selection.validateTimeSlots();
        } else {
            selection.validateAllDayDates();
        }

        return selection;
    }

    /**
     * 이전 버전 호환을 위한 메서드
     */
    @Deprecated
    public static MeetingUserSelection create(Long meetingId, Long userId, Integer intervalMinutes, Map<String, int[]> selections) {
        return create(meetingId, userId, SelectionType.TIME, intervalMinutes, selections);
    }

    public void updateSelections(Map<String, int[]> selections) {
        this.selections = selections;

        // TimeSlot 검증 (타입에 따라)
        if (this.selectionType == SelectionType.TIME) {
            validateTimeSlots();
        } else {
            validateAllDayDates();
        }
    }

    /**
     * TimeSlot 유효성 검증
     * 도메인 규칙: 모든 슬롯 인덱스는 해당 간격에 맞는 범위여야 함
     */
    private void validateTimeSlots() {
        if (selections == null || selections.isEmpty()) {
            throw new IllegalArgumentException("최소 하나의 시간대를 선택해야 합니다.");
        }

        for (Map.Entry<String, int[]> entry : selections.entrySet()) {
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
        if (selections == null || selections.isEmpty()) {
            throw new IllegalArgumentException("최소 하나의 날짜를 선택해야 합니다.");
        }

        for (Map.Entry<String, int[]> entry : selections.entrySet()) {
            int[] slots = entry.getValue();
            // ALL_DAY 타입은 빈 배열이어야 함
            if (slots != null && slots.length > 0) {
                throw new IllegalArgumentException("일 단위 선택(ALL_DAY)인 경우 시간대는 빈 배열이어야 합니다.");
            }
        }
    }
}

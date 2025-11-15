package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

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
     * 날짜별 선택한 시간대 (slotIndex 배열)
     * 예: {"2024-02-15": [18, 19, 21], "2024-02-16": [22, 23]}
     */
    @Type(JsonType.class)
    @Column(name = "selections", columnDefinition = "json", nullable = false)
    private Map<String, int[]> selections;

    public static MeetingUserSelection create(Long meetingId, Long userId, Map<String, int[]> selections) {
        MeetingUserSelection selection = new MeetingUserSelection();
        selection.meetingId = meetingId;
        selection.userId = userId;
        selection.selections = selections;
        return selection;
    }

    public void updateSelections(Map<String, int[]> selections) {
        this.selections = selections;
    }
}

package com.cover.time2gather.domain.meeting;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 미팅 장소 투표 선택
 * 사용자가 선택한 장소들
 */
@Entity
@Table(name = "meeting_location_selections",
       uniqueConstraints = @UniqueConstraint(
           name = "unique_location_vote",
           columnNames = {"meeting_id", "location_id", "user_id"}
       ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingLocationSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static MeetingLocationSelection create(Long meetingId, Long locationId, Long userId) {
        if (meetingId == null) {
            throw new IllegalArgumentException("미팅 ID는 필수입니다.");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("장소 ID는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }

        MeetingLocationSelection selection = new MeetingLocationSelection();
        selection.meetingId = meetingId;
        selection.locationId = locationId;
        selection.userId = userId;
        selection.createdAt = LocalDateTime.now();
        return selection;
    }
}

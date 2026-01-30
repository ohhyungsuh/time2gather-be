package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 미팅 장소 후보
 * 호스트가 설정한 장소 옵션들
 */
@Entity
@Table(name = "meeting_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static MeetingLocation create(Long meetingId, String name, Integer displayOrder) {
        validateName(name);

        MeetingLocation location = new MeetingLocation();
        location.meetingId = meetingId;
        location.name = name.trim();
        location.displayOrder = displayOrder != null ? displayOrder : 0;
        location.createdAt = LocalDateTime.now();
        return location;
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.LOCATION_NAME_REQUIRED);
        }
        if (name.trim().length() > 200) {
            throw new BusinessException(ErrorCode.LOCATION_NAME_TOO_LONG);
        }
    }

    public void updateName(String name) {
        validateName(name);
        this.name = name.trim();
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }
}

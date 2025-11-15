package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.common.BaseEntity;
import com.cover.time2gather.domain.user.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    @Column(length = 50)
    private String timezone = "Asia/Seoul";

    /**
     * 날짜별 가능한 시간대 (slotIndex 배열)
     * 예: {"2024-02-15": [18, 19, 20, 21], "2024-02-16": [22, 23, 24]}
     */
    @Type(JsonType.class)
    @Column(name = "available_dates", columnDefinition = "json", nullable = false)
    private Map<String, int[]> availableDates;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public static Meeting create(
            String meetingCode,
            String title,
            String description,
            User host,
            String timezone,
            Map<String, int[]> availableDates
    ) {
        Meeting meeting = new Meeting();
        meeting.meetingCode = meetingCode;
        meeting.title = title;
        meeting.description = description;
        meeting.host = host;
        meeting.timezone = timezone != null ? timezone : "Asia/Seoul";
        meeting.availableDates = availableDates;
        meeting.isActive = true;
        return meeting;
    }
}


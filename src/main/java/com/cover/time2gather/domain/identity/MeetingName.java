package com.cover.time2gather.domain.identity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_names", uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "user_id"}))
@Getter @NoArgsConstructor
public class MeetingName {
    @Id private String id;
    @Column(name = "meeting_id", nullable = false) private Long meetingId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private long version;
    public MeetingName(Long meetingId, Long userId, String name, long version) {
        this.id = meetingId + ":" + userId;
        this.meetingId = meetingId; this.userId = userId; this.name = name; this.version = version;
    }
    public void rename(String name, long version) { this.name = name; this.version = version; }
}

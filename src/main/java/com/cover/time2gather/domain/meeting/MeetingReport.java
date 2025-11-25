package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false, unique = true)
    private Long meetingId;

    @Column(name = "summary_text", columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    public static MeetingReport create(Long meetingId, String summaryText) {
        MeetingReport report = new MeetingReport();
        report.meetingId = meetingId;
        report.summaryText = summaryText;
        return report;
    }

    public void updateSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
}

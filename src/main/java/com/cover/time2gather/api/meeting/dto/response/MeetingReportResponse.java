package com.cover.time2gather.api.meeting.dto.response;

import com.cover.time2gather.domain.meeting.MeetingReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Meeting report response")
public class MeetingReportResponse {

    @Schema(description = "Report ID", example = "1")
    private long reportId;

    @Schema(description = "Meeting ID", example = "1")
    private long meetingId;

    @Schema(description = "Summarized report content")
    private String summaryText;

    public static MeetingReportResponse from(MeetingReport report) {
        return new MeetingReportResponse(
                report.getId(),
                report.getMeetingId(),
                report.getSummaryText()
        );
    }
}

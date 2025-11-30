package com.cover.time2gather.api.meeting.dto.response;

import com.cover.time2gather.domain.meeting.MeetingReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "모임 레포트 응답")
public class MeetingReportResponse {

    @Schema(description = "레포트 ID", example = "1")
    private long reportId;

    @Schema(description = "모임 ID", example = "1")
    private long meetingId;

    @Schema(description = "요약된 레포트 내용")
    private String summaryText;

    public static MeetingReportResponse from(MeetingReport report) {
        return new MeetingReportResponse(
                report.getId(),
                report.getMeetingId(),
                report.getSummaryText()
        );
    }
}

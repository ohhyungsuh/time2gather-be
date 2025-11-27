package com.cover.time2gather.domain.meeting.event;

public record ReportGenerateEvent(Long meetingId, Integer retryCount) {

    public static ReportGenerateEvent of(Long meetingId) {
        return new ReportGenerateEvent(meetingId, 0);
    }

    public static ReportGenerateEvent ofRetry(Long meetingId, Integer retryCount) {
        return new ReportGenerateEvent(meetingId, retryCount);
    }
}

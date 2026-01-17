package com.cover.time2gather.domain.meeting.client;

import com.cover.time2gather.domain.meeting.ReportData;

public interface ReportSummaryClient {

    String generateSummary(ReportData reportData);
}

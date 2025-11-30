package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingReport;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.ReportData;
import com.cover.time2gather.domain.meeting.client.ReportSummaryClient;
import com.cover.time2gather.domain.meeting.event.ReportGenerateEvent;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.infra.meeting.MeetingReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.cover.time2gather.domain.meeting.constants.ReportConstants.MAX_RETRY_COUNT;

/**
 * 모임 레포트 생성 비동기 작업 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportWorkerService {

    private final ApplicationEventPublisher eventPublisher;
    private final ReportSummaryClient summaryClient;
    private final ScheduledExecutorService reportRetryScheduler;

    private final ReportDataAggregator reportDataAggregator;

    private final MeetingReportRepository reportRepository;

    @Async("reportTaskExecutor")
    public void generateReportAsync(Long meetingId, Integer currentRetryCount) {
        ReportData reportData;

        try {
            reportData = reportDataAggregator.aggregate(meetingId);
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
            log.error("Permanent failure - Data integrity issue. meetingId={}", meetingId, e);
            return;
        } catch (Exception e) {
            log.error("Failed to load meeting data. meetingId={}, retryCount={}", meetingId, currentRetryCount, e);
            handleRetry(meetingId, currentRetryCount);
            return;
        }

        Meeting meeting = reportData.meeting();
        List<MeetingUserSelection> allSelections = reportData.selections();
        Map<Long, User> userMap = reportData.userMap();

        String summaryText;
        try {
            summaryText = summaryClient.generateSummary(meeting, allSelections, userMap);
            if (summaryText == null || summaryText.isBlank()) {
                log.warn("Received empty summary for meetingId={}", meetingId);
                summaryText = "";
            }
        } catch (Exception e) {
            log.error("Failed to generate summary. meetingId={}, retryCount={}", meetingId, currentRetryCount, e);
            handleRetry(meetingId, currentRetryCount);
            return;
        }

        try {
            saveMeetingReport(meetingId, summaryText);
            log.info("Meeting report saved successfully. meetingId={}", meetingId);
        } catch (Exception e) {
            log.error("Failed to save meeting report. meetingId={}, retryCount={}", meetingId, currentRetryCount, e);
            handleRetry(meetingId, currentRetryCount);
        }
    }

    private void saveMeetingReport(Long meetingId, String summaryText) {
        MeetingReport report = reportRepository.findByMeetingId(meetingId)
                .map(r -> {
                    r.updateSummaryText(summaryText);
                    return r;
                })
                .orElseGet(() -> MeetingReport.create(meetingId, summaryText));
        reportRepository.save(report);
    }

    private void handleRetry(Long meetingId, Integer currentRetryCount) {
        try {
            updateRetryCount(meetingId);
            scheduleRetry(meetingId, currentRetryCount);
        } catch (Exception e) {
            log.error("Failed to schedule retry. meetingId={}, retryCount={}", meetingId, currentRetryCount, e);
        }
    }

    private void updateRetryCount(Long meetingId) {
        try {
            MeetingReport report = reportRepository.findByMeetingId(meetingId).orElse(null);
            if (report != null) {
                report.incrementRetryCount();
                reportRepository.save(report);
            }
        } catch (Exception e) {
            log.error("Failed to update retry count. meetingId={}", meetingId, e);
        }
    }

    private void scheduleRetry(Long meetingId, Integer currentRetryCount) {
        int nextRetryCount = currentRetryCount + 1;

        if (nextRetryCount >= MAX_RETRY_COUNT) {
            log.error("Max retry count reached. meetingId={}, retryCount={}",
                    meetingId, nextRetryCount);
            return;
        }

        long delayMillis = (long) Math.pow(2, nextRetryCount) * 1000;
        log.info("Scheduling retry. meetingId={}, nextRetryCount={}, delayMs={}",
                meetingId, nextRetryCount, delayMillis);

        reportRetryScheduler.schedule(() -> {
            try {
                eventPublisher.publishEvent(ReportGenerateEvent.ofRetry(meetingId, nextRetryCount));
            } catch (Exception e) {
                log.error("Failed to publish retry event. meetingId={}, retryCount={}",
                        meetingId, nextRetryCount, e);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
    }
}

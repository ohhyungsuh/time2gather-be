package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.api.meeting.dto.request.UpsertSummaryRequest;
import com.cover.time2gather.api.meeting.dto.response.UpsertSummaryResponse;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingReport;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.event.ReportGenerateEvent;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.meeting.MeetingReportRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import com.cover.time2gather.util.ReportInputTextBuilder;
import com.cover.time2gather.util.ResourceLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

import static com.cover.time2gather.domain.meeting.constants.ReportConstants.*;

/**
 * 모임 레포트 생성 비동기 작업 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportWorkerService {

    private final MeetingRepository meetingRepository;
    private final MeetingUserSelectionRepository selectionRepository;
    private final MeetingReportRepository reportRepository;
    private final UserRepository userRepository;
    private final RestClient restClient;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${openai.model}")
    private String model;

    @Async("reportTaskExecutor")
    public void generateReportAsync(Long meetingId, Integer currentRetryCount) {
        try {
            String instructions = ResourceLoader.loadTextFile(PROMPT_TEMPLATE_PATH);

            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

            List<MeetingUserSelection> allSelections = selectionRepository.findAllByMeetingId(meetingId);

            Set<Long> userIds = allSelections.stream()
                    .map(MeetingUserSelection::getUserId)
                    .collect(Collectors.toSet());

            Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, user -> user));

            String inputText = ReportInputTextBuilder.build(meeting, allSelections, userMap);

            UpsertSummaryRequest request = new UpsertSummaryRequest(model, inputText, instructions);

            UpsertSummaryResponse response = restClient
                    .post()
                    .uri("/responses")
                    .body(request)
                    .retrieve()
                    .body(UpsertSummaryResponse.class);

            if (response == null || response.getSummary() == null || response.getSummary().isBlank()) {
                log.warn("Received empty summary for meetingId={}", meetingId);
            }

            saveMeetingReport(meetingId, response.getSummary());
            log.info("Meeting report saved successfully. meetingId={}", meetingId);

        } catch (Exception e) {
            log.error("Failed to generate report. meetingId={}, retryCount={}",
                    meetingId, currentRetryCount, e);

            updateRetryCount(meetingId);
            scheduleRetry(meetingId, currentRetryCount);
        }
    }

    private void saveMeetingReport(Long meetingId, String summaryText) {
        MeetingReport report = reportRepository.findByMeetingId(meetingId)
                .map(r -> {
                    r.updateSummaryText(summaryText);
                    return r; }
                )
                .orElseGet(() -> MeetingReport.create(meetingId, summaryText));
        reportRepository.save(report);
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

        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                eventPublisher.publishEvent(ReportGenerateEvent.ofRetry(meetingId, nextRetryCount));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Retry scheduling interrupted. meetingId={}", meetingId, e);
            } catch (Exception e) {
                log.error("Failed to publish retry event. meetingId={}, retryCount={}",
                        meetingId, nextRetryCount, e);
            }
        }).start();
    }
}

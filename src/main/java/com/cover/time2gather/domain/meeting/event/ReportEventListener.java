package com.cover.time2gather.domain.meeting.event;

import com.cover.time2gather.domain.meeting.service.ReportWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.RejectedExecutionException;

/**
 * 모임 레포트 생성 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportEventListener {

    private final ReportWorkerService reportWorkerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportGenerateEvent(ReportGenerateEvent event) {
        try {
            reportWorkerService.generateReportAsync(event.meetingId(), event.retryCount());
            log.info("Report generation task queued. meetingId={}, retryCount={}",
                    event.meetingId(), event.retryCount());
        } catch (RejectedExecutionException e) {
            log.error("Report task queue is full. meetingId={}, retryCount={}",
                    event.meetingId(), event.retryCount(), e);
        }
    }
}

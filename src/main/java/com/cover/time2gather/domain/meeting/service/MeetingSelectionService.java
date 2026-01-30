package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingReport;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.event.ReportGenerateEvent;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.meeting.MeetingReportRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MeetingSelectionService {

    private final MeetingUserSelectionRepository selectionRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingReportRepository reportRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Map<String, int[]> getUserSelections(Long meetingId, Long userId) {
        return selectionRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(MeetingUserSelection::getSelections)
                .orElse(Collections.emptyMap());
    }

    @Transactional
    public void upsertUserSelections(Long meetingId, Long userId, Map<String, int[]> selections) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        if (meeting.isConfirmed()) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_CONFIRMED);
        }

        validateSelections(meeting, selections);

        MeetingUserSelection selection = selectionRepository
                .findByMeetingIdAndUserId(meetingId, userId)
                .orElse(null);

        if (selection == null) {
            // Meeting의 selectionType과 intervalMinutes를 전달
            selection = MeetingUserSelection.create(
                    meetingId,
                    userId,
                    meeting.getSelectionType(),
                    meeting.getIntervalMinutes(),
                    selections
            );
            selectionRepository.save(selection);
        } else {
            selection.updateSelections(selections);
        }

        try {
            eventPublisher.publishEvent(ReportGenerateEvent.of(meetingId));
        } catch (Exception e) {
            log.error("Failed to publish report generate event. meetingId={}", meetingId, e);
        }
    }

    private void validateSelections(Meeting meeting, Map<String, int[]> selections) {
        Map<String, int[]> availableDates = meeting.getAvailableDates();

        for (Map.Entry<String, int[]> entry : selections.entrySet()) {
            String date = entry.getKey();
            int[] selectedSlots = entry.getValue();

            // 해당 날짜가 available_dates에 없으면 에러
            if (!availableDates.containsKey(date)) {
                throw new BusinessException(ErrorCode.SELECTION_DATE_NOT_AVAILABLE, date);
            }

            // 빈 배열: ALL_DAY 타입에서만 허용
            if (selectedSlots != null && selectedSlots.length == 0) {
                if (meeting.getSelectionType() != com.cover.time2gather.domain.meeting.SelectionType.ALL_DAY) {
                    throw new BusinessException(ErrorCode.SELECTION_SLOT_EMPTY_FOR_TIME);
                }
                // ALL_DAY 타입이면 빈 배열 허용
                continue;
            }

            // TIME 타입: 시간대 검증
            if (selectedSlots == null) {
                throw new BusinessException(ErrorCode.SELECTION_SLOT_NULL);
            }

            // 선택한 슬롯이 모두 available_dates에 포함되어 있는지 확인
            int[] availableSlots = availableDates.get(date);
            Set<Integer> availableSet = new HashSet<>();
            for (int slot : availableSlots) {
                availableSet.add(slot);
            }

            for (int selectedSlot : selectedSlots) {
                if (!availableSet.contains(selectedSlot)) {
                    throw new BusinessException(ErrorCode.SELECTION_SLOT_NOT_AVAILABLE, selectedSlot, date);
                }
            }
        }
    }

    public List<MeetingUserSelection> getAllSelections(Long meetingId) {
        return selectionRepository.findAllByMeetingId(meetingId);
    }

    public MeetingReport getMeetingReport(Long meetingId) {
        return reportRepository.findByMeetingId(meetingId)
                .orElse(null);
    }
}

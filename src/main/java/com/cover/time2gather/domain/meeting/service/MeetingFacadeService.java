package com.cover.time2gather.domain.meeting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingFacadeService {

    private final MeetingService meetingService;
    private final MeetingSelectionService selectionService;

    @Transactional
    public void upsertUserSelections(String meetingCode, Long userId, Map<String, int[]> slotIndexes) {
        Long meetingId = meetingService.getMeetingByCode(meetingCode).getId();
        selectionService.upsertUserSelections(meetingId, userId, slotIndexes);
    }
}

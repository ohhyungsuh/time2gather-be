package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.MeetingUserSelectionRepository;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingSelectionService {

    private final MeetingUserSelectionRepository selectionRepository;
    private final UserRepository userRepository;

    public Map<String, int[]> getUserSelections(Meeting meeting, Long userId) {
        return selectionRepository.findByMeetingAndUserId(meeting, userId)
                .map(MeetingUserSelection::getSelections)
                .orElse(Collections.emptyMap());
    }

    @Transactional
    public void upsertUserSelections(Meeting meeting, Long userId, Map<String, int[]> selections) {
        // 선택한 시간이 모임의 available_dates 내에 있는지 검증
        validateSelections(meeting, selections);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        MeetingUserSelection selection = selectionRepository
                .findByMeetingAndUserId(meeting, userId)
                .orElse(null);

        if (selection == null) {
            // 새로 생성
            selection = MeetingUserSelection.create(meeting, user, selections);
            selectionRepository.save(selection);
        } else {
            // 업데이트
            selection.updateSelections(selections);
        }
    }

    private void validateSelections(Meeting meeting, Map<String, int[]> selections) {
        Map<String, int[]> availableDates = meeting.getAvailableDates();

        for (Map.Entry<String, int[]> entry : selections.entrySet()) {
            String date = entry.getKey();
            int[] selectedSlots = entry.getValue();

            // 해당 날짜가 available_dates에 없으면 에러
            if (!availableDates.containsKey(date)) {
                throw new IllegalArgumentException("Date " + date + " is not available in this meeting");
            }

            // 선택한 슬롯이 모두 available_dates에 포함되어 있는지 확인
            int[] availableSlots = availableDates.get(date);
            Set<Integer> availableSet = new HashSet<>();
            for (int slot : availableSlots) {
                availableSet.add(slot);
            }

            for (int selectedSlot : selectedSlots) {
                if (!availableSet.contains(selectedSlot)) {
                    throw new IllegalArgumentException(
                            "Slot " + selectedSlot + " is not available on " + date);
                }
            }
        }
    }

    public List<MeetingUserSelection> getAllSelections(Meeting meeting) {
        return selectionRepository.findAllByMeeting(meeting);
    }
}


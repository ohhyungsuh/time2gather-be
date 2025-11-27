package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.api.meeting.dto.request.UpsertSummaryRequest;
import com.cover.time2gather.api.meeting.dto.response.UpsertSummaryResponse;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingReport;
import com.cover.time2gather.infra.meeting.MeetingReportRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MeetingSelectionService {

    private static final String PROMPT_TEMPLATE_PATH = "prompts/meeting-summary-kr.txt";
    private static final String INPUT_MEETING_TITLE = "Meeting Title: ";
    private static final String INPUT_MEETING_DESCRIPTION = "Meeting Description: ";
    private static final String INPUT_MEETING_HOST = "Meeting Host: ";
    private static final String INPUT_VOTED_PARTICIPANTS = "Voted Participants: ";
    private static final String INPUT_PARTICIPANT_SELECTIONS = "Participant Selections:\n";
    private static final String UNKNOWN_USER = "Unknown";

    private final MeetingUserSelectionRepository selectionRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingReportRepository reportRepository;

    private final WebClient webClient;

    @Value("${openai.model}")
    private String model;

    public Map<String, int[]> getUserSelections(Long meetingId, Long userId) {
        return selectionRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(MeetingUserSelection::getSelections)
                .orElse(Collections.emptyMap());
    }

    @Transactional
    public void upsertUserSelections(Long meetingId, Long userId, Map<String, int[]> selections) {
        // 사용자가 존재하는지 검증
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        // 모임이 존재하는지 검증 및 선택한 시간이 모임의 available_dates 내에 있는지 검증
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));
        validateSelections(meeting, selections);

        MeetingUserSelection selection = selectionRepository
                .findByMeetingIdAndUserId(meetingId, userId)
                .orElse(null);

        if (selection == null) {
            selection = MeetingUserSelection.create(meetingId, userId, selections);
            selectionRepository.save(selection);
        } else {
            selection.updateSelections(selections);
        }

        generateMeetingReport(meetingId);
    }

    private void generateMeetingReport(Long meetingId) {
        String instructions = loadPromptTemplate();

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

        List<MeetingUserSelection> allSelections = selectionRepository.findAllByMeetingId(meetingId);

        Set<Long> userIds = allSelections.stream()
                .map(MeetingUserSelection::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        String inputText = buildInputText(meeting, allSelections, userMap);

        UpsertSummaryRequest request = new UpsertSummaryRequest(model, inputText, instructions);

        webClient
                .post()
                .uri("/responses")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UpsertSummaryResponse.class)
                .subscribe(
                        response -> {
                            String summary = response.getSummary();
                            if (summary != null) {
                                saveMeetingReport(meetingId, summary);
                                log.info("Meeting report saved successfully for meetingId: {}", meetingId);
                            }
                        },
                        error -> {
                            log.error("Error generating meeting report for meetingId: {}", meetingId, error);
                        }
                );
    }

    @Transactional
    public void saveMeetingReport(Long meetingId, String summaryText) {
        MeetingReport report = reportRepository.findByMeetingId(meetingId)
                .orElse(null);

        if (report == null) {
            report = MeetingReport.create(meetingId, summaryText);
            reportRepository.save(report);
        } else {
            report.updateSummaryText(summaryText);
        }
    }

    private String loadPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource(PROMPT_TEMPLATE_PATH);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt template", e);
            throw new RuntimeException("Failed to load prompt template", e);
        }
    }

    private String buildInputText(Meeting meeting, List<MeetingUserSelection> selections, Map<Long, User> userMap) {
        StringBuilder sb = new StringBuilder();
        sb.append(INPUT_MEETING_TITLE).append(meeting.getTitle()).append("\n");
        sb.append(INPUT_MEETING_DESCRIPTION).append(meeting.getDescription()).append("\n");

        User host = userMap.get(meeting.getHostUserId());
        String hostName = host != null ? host.getUsername() : UNKNOWN_USER;
        sb.append(INPUT_MEETING_HOST).append(hostName).append("\n");

        sb.append(INPUT_VOTED_PARTICIPANTS).append(selections.size()).append("\n\n");
        sb.append(INPUT_PARTICIPANT_SELECTIONS);

        for (MeetingUserSelection selection : selections) {
            User user = userMap.get(selection.getUserId());
            String username = user != null ? user.getUsername() : UNKNOWN_USER;
            sb.append("- ").append(username).append(":\n");

            Map<String, int[]> userSelections = selection.getSelections();
            for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                String date = entry.getKey();
                int[] slots = entry.getValue();
                sb.append("  * ").append(date).append(": ").append(Arrays.toString(slots)).append("\n");
            }
        }

        return sb.toString();

        // TODO 투표 후 모임 요약 리포트 gpt 생성 비동기 처리


    }

    private void validateSelections(Meeting meeting, Map<String, int[]> selections) {
        Map<String, int[]> availableDates = meeting.getAvailableDates();

        for (Map.Entry<String, int[]> entry : selections.entrySet()) {
            String date = entry.getKey();
            int[] selectedSlots = entry.getValue();

            // 빈 배열인 경우 검증 스킵 (선택 취소를 의미)
            if (selectedSlots == null || selectedSlots.length == 0) {
                continue;
            }

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

    public List<MeetingUserSelection> getAllSelections(Long meetingId) {
        return selectionRepository.findAllByMeetingId(meetingId);
    }

    public MeetingReport getMeetingReport(Long meetingId) {
        return reportRepository.findByMeetingId(meetingId)
                .orElse(null);
    }
}

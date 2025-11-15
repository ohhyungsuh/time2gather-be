package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.api.meeting.dto.*;
import com.cover.time2gather.config.security.JwtAuthentication;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.service.MeetingSelectionService;
import com.cover.time2gather.domain.meeting.service.MeetingService;
import com.cover.time2gather.util.TimeSlotConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "Meeting", description = "모임 관련 API")
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingSelectionService selectionService;

    @PostMapping
    @Operation(summary = "모임 생성", description = "새로운 모임을 생성합니다.")
    public ApiResponse<CreateMeetingResponse> createMeeting(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        // API의 "HH:mm" 문자열을 slotIndex로 변환
        Map<String, int[]> availableDates = convertTimeStringsToSlotIndexes(request.getAvailableDates());

        Meeting meeting = meetingService.createMeeting(
                authentication.getUserId(),
                request.getTitle(),
                request.getDescription(),
                request.getTimezone(),
                availableDates
        );

        CreateMeetingResponse response = new CreateMeetingResponse(
                meeting.getId(),
                meeting.getMeetingCode(),
                "https://when2meet.com/" + meeting.getMeetingCode() // TODO: 실제 도메인으로 변경
        );

        return ApiResponse.success(response);
    }

    @GetMapping("/{meetingCode}")
    @Operation(summary = "모임 상세 조회", description = "모임 상세 정보를 조회합니다. (인증 불필요)")
    public ApiResponse<MeetingDetailResponse> getMeetingDetail(
            @PathVariable String meetingCode
    ) {
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);
        List<MeetingUserSelection> selections = selectionService.getAllSelections(meeting);

        // 참여자 목록 구성
        Set<Long> participantIds = selections.stream()
                .map(s -> s.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, MeetingDetailResponse.ParticipantInfo> participantMap = selections.stream()
                .collect(Collectors.toMap(
                        s -> s.getUser().getId(),
                        s -> new MeetingDetailResponse.ParticipantInfo(
                                s.getUser().getId(),
                                s.getUser().getUsername(),
                                s.getUser().getProfileImageUrl()
                        ),
                        (existing, replacement) -> existing
                ));

        List<MeetingDetailResponse.ParticipantInfo> participants = new ArrayList<>(participantMap.values());

        // schedule 구성: 날짜 -> 시간 -> 유저 목록
        Map<String, Map<String, List<MeetingDetailResponse.ParticipantInfo>>> schedule = buildSchedule(selections);

        // summary 구성
        MeetingDetailResponse.SummaryInfo summary = buildSummary(selections, participantIds.size());

        // meeting 정보 구성
        MeetingDetailResponse.MeetingInfo meetingInfo = new MeetingDetailResponse.MeetingInfo(
                meeting.getId(),
                meeting.getMeetingCode(),
                meeting.getTitle(),
                meeting.getDescription(),
                new MeetingDetailResponse.HostInfo(
                        meeting.getHost().getId(),
                        meeting.getHost().getUsername(),
                        meeting.getHost().getProfileImageUrl()
                ),
                meeting.getTimezone(),
                convertSlotIndexesToTimeStrings(meeting.getAvailableDates())
        );

        MeetingDetailResponse response = new MeetingDetailResponse(
                meetingInfo,
                participants,
                schedule,
                summary
        );

        return ApiResponse.success(response);
    }

    @GetMapping("/{meetingCode}/selections")
    @Operation(summary = "내 선택 조회", description = "현재 사용자의 시간 선택을 조회합니다.")
    public ApiResponse<UserSelectionResponse> getUserSelections(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode
    ) {
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);
        Map<String, int[]> selections = selectionService.getUserSelections(meeting, authentication.getUserId());

        Map<String, String[]> timeStrSelections = convertSlotIndexesToTimeStrings(selections);

        return ApiResponse.success(new UserSelectionResponse(timeStrSelections));
    }

    @PutMapping("/{meetingCode}/selections")
    @Operation(summary = "시간 선택/수정", description = "사용자의 시간 선택을 등록하거나 수정합니다.")
    public ApiResponse<Void> upsertUserSelections(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode,
            @Valid @RequestBody UpsertUserSelectionRequest request
    ) {
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);

        // "HH:mm" 문자열을 slotIndex로 변환
        Map<String, int[]> selections = convertTimeStringsToSlotIndexes(request.getSelections());

        selectionService.upsertUserSelections(meeting, authentication.getUserId(), selections);

        return ApiResponse.success(null);
    }

    // ===== 헬퍼 메서드들 =====

    private Map<String, int[]> convertTimeStringsToSlotIndexes(Map<String, String[]> timeStrings) {
        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, String[]> entry : timeStrings.entrySet()) {
            String date = entry.getKey();
            String[] times = entry.getValue();
            int[] slots = Arrays.stream(times)
                    .mapToInt(TimeSlotConverter::timeStrToSlotIndex)
                    .toArray();
            result.put(date, slots);
        }
        return result;
    }

    private Map<String, String[]> convertSlotIndexesToTimeStrings(Map<String, int[]> slotIndexes) {
        Map<String, String[]> result = new HashMap<>();
        for (Map.Entry<String, int[]> entry : slotIndexes.entrySet()) {
            String date = entry.getKey();
            int[] slots = entry.getValue();
            String[] times = Arrays.stream(slots)
                    .mapToObj(TimeSlotConverter::slotIndexToTimeStr)
                    .toArray(String[]::new);
            result.put(date, times);
        }
        return result;
    }

    private Map<String, Map<String, List<MeetingDetailResponse.ParticipantInfo>>> buildSchedule(
            List<MeetingUserSelection> selections
    ) {
        Map<String, Map<String, List<MeetingDetailResponse.ParticipantInfo>>> schedule = new HashMap<>();

        for (MeetingUserSelection selection : selections) {
            MeetingDetailResponse.ParticipantInfo participant = new MeetingDetailResponse.ParticipantInfo(
                    selection.getUser().getId(),
                    selection.getUser().getUsername(),
                    selection.getUser().getProfileImageUrl()
            );

            Map<String, int[]> userSelections = selection.getSelections();
            for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                String date = entry.getKey();
                int[] slots = entry.getValue();

                schedule.putIfAbsent(date, new HashMap<>());
                Map<String, List<MeetingDetailResponse.ParticipantInfo>> timeMap = schedule.get(date);

                for (int slot : slots) {
                    String time = TimeSlotConverter.slotIndexToTimeStr(slot);
                    timeMap.putIfAbsent(time, new ArrayList<>());
                    timeMap.get(time).add(participant);
                }
            }
        }

        return schedule;
    }

    private MeetingDetailResponse.SummaryInfo buildSummary(
            List<MeetingUserSelection> selections,
            int totalParticipants
    ) {
        // 날짜-시간별 카운트
        Map<String, Map<Integer, Integer>> countMap = new HashMap<>();

        for (MeetingUserSelection selection : selections) {
            Map<String, int[]> userSelections = selection.getSelections();
            for (Map.Entry<String, int[]> entry : userSelections.entrySet()) {
                String date = entry.getKey();
                int[] slots = entry.getValue();

                countMap.putIfAbsent(date, new HashMap<>());
                Map<Integer, Integer> slotCountMap = countMap.get(date);

                for (int slot : slots) {
                    slotCountMap.put(slot, slotCountMap.getOrDefault(slot, 0) + 1);
                }
            }
        }

        // bestSlots 찾기 (가장 많은 사람이 가능한 시간대)
        List<MeetingDetailResponse.BestSlot> bestSlots = new ArrayList<>();
        int maxCount = 0;

        for (Map.Entry<String, Map<Integer, Integer>> dateEntry : countMap.entrySet()) {
            String date = dateEntry.getKey();
            Map<Integer, Integer> slotCountMap = dateEntry.getValue();

            for (Map.Entry<Integer, Integer> slotEntry : slotCountMap.entrySet()) {
                int slot = slotEntry.getKey();
                int count = slotEntry.getValue();

                if (count > maxCount) {
                    maxCount = count;
                    bestSlots.clear();
                    bestSlots.add(new MeetingDetailResponse.BestSlot(
                            date,
                            TimeSlotConverter.slotIndexToTimeStr(slot),
                            count,
                            totalParticipants > 0 ? (count * 100.0 / totalParticipants) : 0
                    ));
                } else if (count == maxCount) {
                    bestSlots.add(new MeetingDetailResponse.BestSlot(
                            date,
                            TimeSlotConverter.slotIndexToTimeStr(slot),
                            count,
                            totalParticipants > 0 ? (count * 100.0 / totalParticipants) : 0
                    ));
                }
            }
        }

        return new MeetingDetailResponse.SummaryInfo(totalParticipants, bestSlots);
    }
}


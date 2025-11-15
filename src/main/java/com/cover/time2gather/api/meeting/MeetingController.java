package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.api.meeting.dto.*;
import com.cover.time2gather.config.security.JwtAuthentication;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.service.MeetingSelectionService;
import com.cover.time2gather.domain.meeting.service.MeetingService;
import com.cover.time2gather.domain.user.User;
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
        // DTO → 도메인 모델 변환
        Map<String, int[]> availableDates = convertTimeStringsToSlotIndexes(request.getAvailableDates());

        // Service 호출 (비즈니스 로직)
        Meeting meeting = meetingService.createMeeting(
                authentication.getUserId(),
                request.getTitle(),
                request.getDescription(),
                request.getTimezone(),
                availableDates
        );

        // 도메인 모델 → DTO 변환
        CreateMeetingResponse response = new CreateMeetingResponse(
                meeting.getId(),
                meeting.getMeetingCode(),
                "https://time2gather.org/" + meeting.getMeetingCode()
        );

        return ApiResponse.success(response);
    }

    @GetMapping("/{meetingCode}")
    @Operation(summary = "모임 상세 조회", description = "모임 상세 정보를 조회합니다. (인증 불필요)")
    public ApiResponse<MeetingDetailResponse> getMeetingDetail(
            @PathVariable String meetingCode
    ) {
        // Service 호출 (비즈니스 로직)
        MeetingDetailData detailData = meetingService.getMeetingDetailData(meetingCode);

        // 도메인 모델 → DTO 변환
        MeetingDetailResponse response = convertToMeetingDetailResponse(detailData);

        return ApiResponse.success(response);
    }

    @GetMapping("/{meetingCode}/selections")
    @Operation(summary = "내 선택 조회", description = "현재 사용자의 시간 선택을 조회합니다.")
    public ApiResponse<UserSelectionResponse> getUserSelections(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode
    ) {
        // Service 호출
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);
        Map<String, int[]> selections = selectionService.getUserSelections(meeting.getId(), authentication.getUserId());

        // 도메인 모델 → DTO 변환
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
        // DTO → 도메인 모델 변환
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);
        Map<String, int[]> selections = convertTimeStringsToSlotIndexes(request.getSelections());

        // Service 호출 (비즈니스 로직)
        selectionService.upsertUserSelections(meeting.getId(), authentication.getUserId(), selections);

        return ApiResponse.success(null);
    }

    // ===== DTO 변환 메서드 (Controller 책임) =====

    /**
     * 도메인 모델 → MeetingDetailResponse DTO 변환
     */
    private MeetingDetailResponse convertToMeetingDetailResponse(MeetingDetailData detailData) {
        Meeting meeting = detailData.getMeeting();
        User host = detailData.getHost();

        // Meeting 정보 변환
        MeetingDetailResponse.MeetingInfo meetingInfo = new MeetingDetailResponse.MeetingInfo(
                meeting.getId(),
                meeting.getMeetingCode(),
                meeting.getTitle(),
                meeting.getDescription(),
                new MeetingDetailResponse.HostInfo(
                        host.getId(),
                        host.getUsername(),
                        host.getProfileImageUrl()
                ),
                meeting.getTimezone(),
                convertSlotIndexesToTimeStrings(meeting.getAvailableDates())
        );

        // 참여자 정보 변환
        List<MeetingDetailResponse.ParticipantInfo> participants = detailData.getParticipants().stream()
                .map(user -> new MeetingDetailResponse.ParticipantInfo(
                        user.getId(),
                        user.getUsername(),
                        user.getProfileImageUrl()
                ))
                .collect(Collectors.toList());

        // Schedule 정보 변환 (slotIndex → "HH:mm")
        Map<String, Map<String, List<MeetingDetailResponse.ParticipantInfo>>> schedule = convertScheduleToResponse(
                detailData.getSchedule()
        );

        // Summary 정보 변환 (slotIndex → "HH:mm")
        MeetingDetailResponse.SummaryInfo summary = convertSummaryToResponse(
                detailData.getSummary()
        );

        return new MeetingDetailResponse(meetingInfo, participants, schedule, summary);
    }

    /**
     * Schedule 도메인 모델 → DTO 변환
     */
    private Map<String, Map<String, List<MeetingDetailResponse.ParticipantInfo>>> convertScheduleToResponse(
            MeetingDetailData.ScheduleData scheduleData
    ) {
        Map<String, Map<String, List<MeetingDetailResponse.ParticipantInfo>>> result = new HashMap<>();

        Map<String, Map<Integer, List<User>>> dateTimeUserMap = scheduleData.getDateTimeUserMap();
        for (Map.Entry<String, Map<Integer, List<User>>> dateEntry : dateTimeUserMap.entrySet()) {
            String date = dateEntry.getKey();
            Map<Integer, List<User>> slotUserMap = dateEntry.getValue();

            result.putIfAbsent(date, new HashMap<>());
            Map<String, List<MeetingDetailResponse.ParticipantInfo>> timeUserMap = result.get(date);

            for (Map.Entry<Integer, List<User>> slotEntry : slotUserMap.entrySet()) {
                int slot = slotEntry.getKey();
                List<User> users = slotEntry.getValue();

                String time = TimeSlotConverter.slotIndexToTimeStr(slot);
                List<MeetingDetailResponse.ParticipantInfo> participantInfos = users.stream()
                        .map(user -> new MeetingDetailResponse.ParticipantInfo(
                                user.getId(),
                                user.getUsername(),
                                user.getProfileImageUrl()
                        ))
                        .collect(Collectors.toList());

                timeUserMap.put(time, participantInfos);
            }
        }

        return result;
    }

    /**
     * Summary 도메인 모델 → DTO 변환
     */
    private MeetingDetailResponse.SummaryInfo convertSummaryToResponse(
            MeetingDetailData.SummaryData summaryData
    ) {
        List<MeetingDetailResponse.BestSlot> bestSlots = summaryData.getBestSlots().stream()
                .map(slot -> new MeetingDetailResponse.BestSlot(
                        slot.getDate(),
                        TimeSlotConverter.slotIndexToTimeStr(slot.getSlotIndex()),
                        slot.getCount(),
                        slot.getPercentage()
                ))
                .collect(Collectors.toList());

        return new MeetingDetailResponse.SummaryInfo(summaryData.getTotalParticipants(), bestSlots);
    }

    /**
     * API "HH:mm" → slotIndex 변환
     */
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

    /**
     * slotIndex → API "HH:mm" 변환
     */
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
}

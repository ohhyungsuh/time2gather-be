package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.api.meeting.dto.*;
import com.cover.time2gather.config.security.JwtAuthentication;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.MeetingReport;
import com.cover.time2gather.domain.meeting.service.MeetingSelectionService;
import com.cover.time2gather.domain.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        // Service 호출 (비즈니스 로직)
        Meeting meeting = meetingService.createMeeting(
                authentication.getUserId(),
                request.getTitle(),
                request.getDescription(),
                request.getTimezone(),
                request.toSlotIndexes()  // DTO에서 변환
        );

        // 도메인 → DTO 변환
        return ApiResponse.success(CreateMeetingResponse.from(meeting));
    }

    @GetMapping("/{meetingCode}")
    @Operation(summary = "모임 상세 조회", description = "모임 상세 정보를 조회합니다. (인증 불필요)")
    public ApiResponse<MeetingDetailResponse> getMeetingDetail(
            @PathVariable String meetingCode
    ) {
        // Service 호출 (비즈니스 로직)
        MeetingDetailData detailData = meetingService.getMeetingDetailData(meetingCode);

        // 도메인 → DTO 변환
        return ApiResponse.success(MeetingDetailResponse.from(detailData));
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

        // 도메인 → DTO 변환
        return ApiResponse.success(UserSelectionResponse.from(selections));
    }

    @PutMapping("/{meetingCode}/selections")
    @Operation(summary = "시간 선택/수정", description = "사용자의 시간 선택을 등록하거나 수정합니다.")
    public ApiResponse<Void> upsertUserSelections(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode,
            @Valid @RequestBody UpsertUserSelectionRequest request
    ) {
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);
        selectionService.upsertUserSelections(
                meeting.getId(),
                authentication.getUserId(),
                request.toSlotIndexes()
        );

        return ApiResponse.success(null);
    }

    @GetMapping("/{meetingCode}/report")
    @Operation(summary = "모임 레포트 조회", description = "AI가 생성한 모임 요약 레포트를 조회합니다. (인증 불필요)")
    public ApiResponse<MeetingReportResponse> getMeetingReport(
            @PathVariable String meetingCode
    ) {
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);
        MeetingReport report = selectionService.getMeetingReport(meeting.getId());

        if (report == null) {
            return ApiResponse.success(null);
        }

        return ApiResponse.success(MeetingReportResponse.from(report));
    }
}


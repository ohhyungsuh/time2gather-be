package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.api.meeting.dto.ConfirmMeetingRequest;
import com.cover.time2gather.api.meeting.dto.request.AddLocationRequest;
import com.cover.time2gather.api.meeting.dto.request.ConfirmLocationRequest;
import com.cover.time2gather.api.meeting.dto.request.CreateMeetingRequest;
import com.cover.time2gather.api.meeting.dto.request.UpsertUserSelectionRequest;
import com.cover.time2gather.api.meeting.dto.request.VoteLocationsRequest;
import com.cover.time2gather.api.meeting.dto.response.CreateMeetingResponse;
import com.cover.time2gather.api.meeting.dto.response.LocationResponse;
import com.cover.time2gather.api.meeting.dto.response.MeetingDetailResponse;
import com.cover.time2gather.api.meeting.dto.response.MeetingReportResponse;
import com.cover.time2gather.api.meeting.dto.response.UserLocationSelectionsResponse;
import com.cover.time2gather.api.meeting.dto.response.UserSelectionResponse;
import com.cover.time2gather.config.security.JwtAuthentication;
import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.MeetingLocation;
import com.cover.time2gather.domain.meeting.MeetingReport;
import com.cover.time2gather.domain.meeting.service.MeetingFacadeService;
import com.cover.time2gather.domain.meeting.service.CalendarExportService;
import com.cover.time2gather.domain.meeting.service.MeetingLocationService;
import com.cover.time2gather.domain.meeting.service.MeetingSelectionService;
import com.cover.time2gather.domain.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "Meeting", description = "Meeting management APIs")
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingSelectionService selectionService;
    private final MeetingFacadeService meetingFacadeService;
    private final CalendarExportService calendarExportService;
    private final MeetingLocationService locationService;

    @PostMapping
    @Operation(summary = "Create meeting", description = "Creates a new meeting.")
    public ApiResponse<CreateMeetingResponse> createMeeting(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        // 장소 검증
        request.validateLocations();

        // Service 호출 (비즈니스 로직)
        Meeting meeting = meetingService.createMeeting(
                authentication.getUserId(),
                request.getTitle(),
                request.getDescription(),
                request.getTimezone(),
                request.getSelectionTypeEnum(),
                request.getIntervalMinutes(),
                request.toSlotIndexes(),  // DTO에서 변환
                request.isLocationVoteEnabled(),
                request.getLocations()
        );

        // 도메인 → DTO 변환
        return ApiResponse.success(CreateMeetingResponse.from(meeting));
    }

    @GetMapping("/{meetingCode}")
    @Operation(summary = "Get meeting detail", description = "Retrieves meeting detail information. (Authentication optional)")
    public ApiResponse<MeetingDetailResponse> getMeetingDetail(
            @PathVariable String meetingCode,
            @AuthenticationPrincipal JwtAuthentication authentication
    ) {
        // Service 호출 (비즈니스 로직)
        Long currentUserId = authentication != null ? authentication.getUserId() : null;
        MeetingDetailData detailData = meetingService.getMeetingDetailData(meetingCode, currentUserId);

        // 도메인 → DTO 변환
        return ApiResponse.success(MeetingDetailResponse.from(detailData));
    }

    @GetMapping("/{meetingCode}/selections")
    @Operation(summary = "Get my selections", description = "Retrieves current user's time selections.")
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
    @Operation(
        summary = "Create/Update time selections",
        description = """
            ## Create or update user's time selections
            
            ### Usage flow
            1. Call `GET /meetings/{code}` to get meeting info
            2. Check `meeting.selectionType` ("TIME" or "ALL_DAY")
            3. Build selections matching that type
            4. Call this API
            
            ---
            
            ### Type-specific usage
            
            #### 1. TIME type (time-slot selection)
            When meeting uses time-slot selection:
            ```json
            {
              "selections": [
                {
                  "date": "2024-12-15",
                  "type": "TIME",
                  "times": ["09:00", "10:00", "11:00"]
                },
                {
                  "date": "2024-12-16",
                  "type": "TIME",
                  "times": ["14:00", "15:00"]
                }
              ]
            }
            ```
            
            **Requirements:**
            - `type` = "TIME"
            - `times` array required (at least 1 time)
            - Empty array [] or null for `times` causes error!
            
            #### 2. ALL_DAY type (full-day selection)
            When meeting uses full-day selection:
            ```json
            {
              "selections": [
                {
                  "date": "2024-12-20",
                  "type": "ALL_DAY"
                },
                {
                  "date": "2024-12-21",
                  "type": "ALL_DAY"
                }
              ]
            }
            ```
            
            **Requirements:**
            - `type` = "ALL_DAY"
            - `times` field is ignored (null, [], anything works)
            
            ---
            
            ### Important notes
            
            1. **Meeting type and selection type must match**
               - TIME meeting -> use type="TIME"
               - ALL_DAY meeting -> use type="ALL_DAY"
               - Mismatch causes server error
            
            2. **Exclude unselected dates from array**
               - Unselected date = don't include in selections array
               - Don't send null or empty objects
            
            3. **Replaces existing selections**
               - This API completely replaces existing selections
               - Not a partial update, but full replacement
            
            ---
            
            ### Common mistakes
            
            **Mistake 1**: Empty times for TIME type
            ```json
            {"date": "2024-12-15", "type": "TIME", "times": []}
            ```
            -> **Error**: "TIME type but no times specified"
            
            **Mistake 2**: Missing type field
            ```json
            {"date": "2024-12-15", "times": ["09:00"]}
            ```
            -> **Error**: "Type is required"
            
            **Mistake 3**: Invalid type value
            ```json
            {"date": "2024-12-15", "type": "FULL_DAY", "times": []}
            ```
            -> **Error**: "Unknown type: FULL_DAY"
            
            ---
            
            ### See Request Body Schema for detailed field descriptions
            """
    )
    public ApiResponse<Void> upsertUserSelections(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode,
            @Valid @RequestBody UpsertUserSelectionRequest request
    ) {

        // 모임 조회하여 intervalMinutes 가져오기
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);

        if (meeting == null) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        Integer intervalMinutes = meeting.getIntervalMinutes();
        if (intervalMinutes == null) {
            throw new BusinessException(ErrorCode.MEETING_NO_INTERVAL_INFO);
        }

        meetingFacadeService.upsertUserSelections(
                meetingCode,
                authentication.getUserId(),
                request.toSlotIndexes(intervalMinutes)
        );

        return ApiResponse.success(null);
    }

    @GetMapping("/{meetingCode}/report")
    @Operation(summary = "Get meeting report", description = "Retrieves AI-generated meeting summary report. (No authentication required)")
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

    @GetMapping("/{meetingCode}/export")
    @Operation(
        summary = "Export to calendar",
        description = """
            Downloads selected time slot as an ICS file.
            - Can be imported into Google Calendar, iOS Calendar, etc.
            - Specifying date and slotIndex creates ICS file for that time slot.
            - Without parameters, uses the first bestSlot (most selected time).
            - slotIndex of -1 creates an ALL_DAY (full-day) event.
            - Error occurs if no one has selected a time yet (when no parameters).
        """
    )
    public ResponseEntity<byte[]> exportToCalendar(
            @PathVariable String meetingCode,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer slotIndex
    ) {
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);

        String targetDate;
        String timeString;

        // date 또는 slotIndex 중 하나만 있는 경우 에러
        if ((date != null && slotIndex == null) || (date == null && slotIndex != null)) {
            throw new BusinessException(ErrorCode.MEETING_DATE_SLOT_TOGETHER);
        }

        if (date != null) {
            // 파라미터가 있으면 해당 날짜/시간으로 ICS 생성
            targetDate = validateAndParseDate(date, meeting);
            timeString = convertSlotIndexToTimeString(slotIndex, meeting.getIntervalMinutes());
        } else {
            // 파라미터가 없으면 기존 로직 (bestSlot 첫번째)
            MeetingDetailData detailData = meetingService.getMeetingDetailData(meetingCode, null);

            if (detailData.getSummary().getBestSlots().isEmpty()) {
                throw new BusinessException(ErrorCode.MEETING_NO_PARTICIPANTS);
            }

            MeetingDetailData.BestSlot bestSlot = detailData.getSummary().getBestSlots().getFirst();
            targetDate = bestSlot.getDate();
            timeString = convertSlotIndexToTimeString(bestSlot.getSlotIndex(), meeting.getIntervalMinutes());
        }

        // ICS 파일 생성
        byte[] icsFile = calendarExportService.createIcsFile(
                meeting.getTitle(),
                meeting.getDescription(),
                targetDate,
                timeString,
                meeting.getTimezone(),
                meeting.getIntervalMinutes()
        );

        // 파일명 생성 (iOS Safari 호환)
        String filename = generateFilename(targetDate, timeString);

        // iOS/macOS에서 캘린더 앱이 자동으로 열리도록 설정
        // - Content-Type: text/calendar → 브라우저가 ICS 파일로 인식
        // - Content-Disposition: inline → 다운로드 대신 앱에서 바로 열기
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .header(HttpHeaders.CONTENT_TYPE, "text/calendar; charset=utf-8")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(icsFile.length))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(icsFile);
    }

    @PutMapping("/{meetingCode}/confirm")
    @Operation(
        summary = "Confirm meeting schedule",
        description = """
            Host confirms the meeting schedule.
            - Only the host can confirm.
            - Already confirmed meetings must be cancelled before re-confirming.
            - After confirmation, participants cannot modify their votes.
            - For ALL_DAY type meetings, pass null for slotIndex.
        """
    )
    public ApiResponse<Void> confirmMeeting(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode,
            @Valid @RequestBody ConfirmMeetingRequest request
    ) {
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);

        // 호스트 권한 확인
        if (!meeting.getHostUserId().equals(authentication.getUserId())) {
            throw new BusinessException(ErrorCode.MEETING_HOST_ONLY);
        }

        // 확정 처리 (도메인 메서드에서 검증 수행)
        meetingService.confirmMeeting(meeting, request.getDate(), request.getSlotIndex());

        return ApiResponse.success(null);
    }

    @DeleteMapping("/{meetingCode}/confirm")
    @Operation(
        summary = "Cancel meeting confirmation",
        description = """
            Host cancels the meeting schedule confirmation.
            - Only the host can cancel.
            - Unconfirmed meetings cannot be cancelled.
            - After cancellation, participants can modify their votes again.
        """
    )
    public ApiResponse<Void> cancelConfirmation(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode
    ) {
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);

        // 호스트 권한 확인
        if (!meeting.getHostUserId().equals(authentication.getUserId())) {
            throw new BusinessException(ErrorCode.MEETING_HOST_ONLY);
        }

        // 취소 처리 (도메인 메서드에서 검증 수행)
        meetingService.cancelConfirmation(meeting);

        return ApiResponse.success(null);
    }

    @PostMapping("/{meetingCode}/locations")
    @Operation(
        summary = "Add location",
        description = """
            Host adds a new location to the meeting.
            - Only the host can add locations.
            - Only available for meetings with location voting enabled.
            - Maximum 5 locations allowed.
        """
    )
    public ApiResponse<LocationResponse> addLocation(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode,
            @Valid @RequestBody AddLocationRequest request
    ) {
        MeetingLocation location = locationService.addLocation(
            meetingCode,
            authentication.getUserId(),
            request.getName()
        );

        return ApiResponse.success(LocationResponse.from(location));
    }

    @DeleteMapping("/{meetingCode}/locations/{locationId}")
    @Operation(
        summary = "Delete location",
        description = """
            Host deletes a location from the meeting.
            - Only the host can delete locations.
            - Only available for meetings with location voting enabled.
            - Minimum 2 locations must be maintained.
            - Votes for the location will also be deleted.
        """
    )
    public ApiResponse<Void> deleteLocation(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode,
            @PathVariable Long locationId
    ) {
        locationService.deleteLocation(
            meetingCode,
            authentication.getUserId(),
            locationId
        );

        return ApiResponse.success(null);
    }

    @PutMapping("/{meetingCode}/location-selections")
    @Operation(
        summary = "Vote for locations",
        description = """
            Saves user's location votes.
            - Multiple selections allowed.
            - Sending empty array removes existing votes (skip voting).
            - Existing votes are completely replaced with new votes.
        """
    )
    public ApiResponse<Void> voteLocations(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode,
            @Valid @RequestBody VoteLocationsRequest request
    ) {
        locationService.voteLocations(
            meetingCode,
            authentication.getUserId(),
            request.getLocationIds()
        );

        return ApiResponse.success(null);
    }

    @GetMapping("/{meetingCode}/location-selections")
    @Operation(
        summary = "Get my location votes",
        description = """
            Retrieves current user's location votes.
            - Returns empty array if user hasn't voted.
        """
    )
    public ApiResponse<UserLocationSelectionsResponse> getMyLocationSelections(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode
    ) {
        List<Long> locationIds = locationService.selectUserLocationIds(
            meetingCode,
            authentication.getUserId()
        );

        return ApiResponse.success(UserLocationSelectionsResponse.from(locationIds));
    }

    @PutMapping("/{meetingCode}/confirm-location")
    @Operation(
        summary = "Confirm location",
        description = """
            Host confirms the meeting location.
            - Only the host can confirm.
            - Already confirmed meetings must be cancelled before re-confirming.
        """
    )
    public ApiResponse<Void> confirmLocation(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode,
            @Valid @RequestBody ConfirmLocationRequest request
    ) {
        locationService.confirmLocation(
            meetingCode,
            authentication.getUserId(),
            request.getLocationId()
        );

        return ApiResponse.success(null);
    }

    @DeleteMapping("/{meetingCode}/confirm-location")
    @Operation(
        summary = "Cancel location confirmation",
        description = """
            Host cancels the meeting location confirmation.
            - Only the host can cancel.
            - Unconfirmed meetings cannot be cancelled.
        """
    )
    public ApiResponse<Void> cancelLocationConfirmation(
            @AuthenticationPrincipal JwtAuthentication authentication,
            @PathVariable String meetingCode
    ) {
        locationService.cancelLocationConfirmation(
            meetingCode,
            authentication.getUserId()
        );

        return ApiResponse.success(null);
    }

    private String validateAndParseDate(String date, Meeting meeting) {
        // 날짜 형식 검증 (yyyy-MM-dd)
        try {
            java.time.LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException(ErrorCode.MEETING_DATE_FORMAT_INVALID);
        }

        // Meeting에 해당 날짜가 있는지 검증
        if (!meeting.getAvailableDates().containsKey(date)) {
            throw new BusinessException(ErrorCode.MEETING_DATE_NOT_AVAILABLE);
        }

        return date;
    }

    private String convertSlotIndexToTimeString(int slotIndex, int intervalMinutes) {
        if (slotIndex == -1) {
            return "ALL_DAY";
        }
        return com.cover.time2gather.domain.meeting.vo.TimeSlot
                .fromIndex(slotIndex, intervalMinutes)
                .toTimeString();
    }

    private String generateFilename(String date, String timeString) {
        if ("ALL_DAY".equals(timeString)) {
            return String.format("meeting_%s_all_day.ics", date);
        }
        return String.format("meeting_%s_%s.ics", date, timeString.replace(":", ""));
    }
}


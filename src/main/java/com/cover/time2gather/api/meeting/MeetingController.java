package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.api.meeting.dto.ExportCalendarRequest;
import com.cover.time2gather.api.meeting.dto.request.CreateMeetingRequest;
import com.cover.time2gather.api.meeting.dto.request.UpsertUserSelectionRequest;
import com.cover.time2gather.api.meeting.dto.response.CreateMeetingResponse;
import com.cover.time2gather.api.meeting.dto.response.MeetingDetailResponse;
import com.cover.time2gather.api.meeting.dto.response.MeetingReportResponse;
import com.cover.time2gather.api.meeting.dto.response.UserSelectionResponse;
import com.cover.time2gather.config.security.JwtAuthentication;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.MeetingReport;
import com.cover.time2gather.domain.meeting.service.MeetingFacadeService;
import com.cover.time2gather.domain.meeting.service.CalendarExportService;
import com.cover.time2gather.domain.meeting.service.MeetingSelectionService;
import com.cover.time2gather.domain.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final MeetingFacadeService meetingFacadeService;
    private final CalendarExportService calendarExportService;

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
                request.getSelectionTypeEnum(),
                request.getIntervalMinutes(),
                request.toSlotIndexes()  // DTO에서 변환
        );

        // 도메인 → DTO 변환
        return ApiResponse.success(CreateMeetingResponse.from(meeting));
    }

    @GetMapping("/{meetingCode}")
    @Operation(summary = "모임 상세 조회", description = "모임 상세 정보를 조회합니다. (인증 선택적)")
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
    @Operation(
        summary = "시간 선택/수정",
        description = """
            ## 사용자의 시간 선택을 등록하거나 수정합니다
            
            ### 📌 사용 흐름
            1. `GET /meetings/{code}`로 모임 정보 조회
            2. `meeting.selectionType` 확인 ("TIME" 또는 "ALL_DAY")
            3. 해당 타입에 맞게 selections 구성
            4. 이 API 호출
            
            ---
            
            ### 🎯 타입별 사용법
            
            #### 1. TIME 타입 (시간 단위 선택)
            모임이 시간 단위로 선택하는 경우:
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
            
            **필수 조건:**
            - `type` = "TIME"
            - `times` 배열 필수 (최소 1개 시간)
            - `times`가 빈 배열 [] 또는 null이면 에러!
            
            #### 2. ALL_DAY 타입 (일 단위 선택)
            모임이 일 단위로 선택하는 경우:
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
            
            **조건:**
            - `type` = "ALL_DAY"
            - `times` 필드는 무시됨 (null, [], 뭐든 가능)
            
            ---
            
            ### ⚠️ 중요 사항
            
            1. **모임 타입과 선택 타입이 일치해야 함**
               - TIME 모임 → type="TIME" 사용
               - ALL_DAY 모임 → type="ALL_DAY" 사용
               - 불일치 시 서버 에러
            
            2. **선택하지 않은 날짜는 배열에서 제외**
               - 선택 안 한 날짜 = selections 배열에 포함하지 않음
               - null이나 빈 객체 보내지 말 것
            
            3. **기존 선택 덮어쓰기**
               - 이 API는 기존 선택을 완전히 대체합니다
               - 부분 수정이 아닌 전체 교체
            
            ---
            
            ### ❌ 흔한 실수
            
            **실수 1**: TIME 타입인데 times가 비어있음
            ```json
            {"date": "2024-12-15", "type": "TIME", "times": []}
            ```
            → **에러**: "TIME 타입인데 시간이 지정되지 않았습니다"
            
            **실수 2**: type 필드 누락
            ```json
            {"date": "2024-12-15", "times": ["09:00"]}
            ```
            → **에러**: "타입은 필수입니다"
            
            **실수 3**: 잘못된 타입 값
            ```json
            {"date": "2024-12-15", "type": "FULL_DAY", "times": []}
            ```
            → **에러**: "알 수 없는 타입: FULL_DAY"
            
            ---
            
            ### 📖 상세 필드 설명은 Request Body Schema 참고
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
            throw new IllegalArgumentException("모임을 찾을 수 없습니다: " + meetingCode);
        }

        Integer intervalMinutes = meeting.getIntervalMinutes();
        if (intervalMinutes == null) {
            throw new IllegalArgumentException("모임의 시간 간격 정보가 없습니다");
        }

        meetingFacadeService.upsertUserSelections(
                meetingCode,
                authentication.getUserId(),
                request.toSlotIndexes(intervalMinutes)
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

    @PostMapping("/{meetingCode}/export")
    @Operation(summary = "캘린더로 export",
               description = "선택한 날짜/시간을 ICS 파일로 다운로드합니다. Google Calendar, iOS Calendar 등에서 import 가능합니다.")
    public ResponseEntity<byte[]> exportToCalendar(
            @PathVariable String meetingCode,
            @Valid @RequestBody ExportCalendarRequest request
    ) {
        Meeting meeting = meetingService.getMeetingByCode(meetingCode);

        // ICS 파일 생성
        byte[] icsFile = calendarExportService.createIcsFile(
                meeting.getTitle(),
                meeting.getDescription(),
                request.getDate(),
                request.getTime(),
                meeting.getTimezone()
        );

        // 파일명 생성 (예: meeting_2024-02-15_1430.ics)
        String filename = String.format("meeting_%s_%s.ics",
                request.getDate(),
                request.getTime().replace(":", ""));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(icsFile);
    }
}


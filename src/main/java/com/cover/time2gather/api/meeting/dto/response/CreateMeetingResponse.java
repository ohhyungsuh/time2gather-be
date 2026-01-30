package com.cover.time2gather.api.meeting.dto.response;

import com.cover.time2gather.domain.meeting.Meeting;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Create meeting response")
public class CreateMeetingResponse {

    @Schema(description = "Meeting ID", example = "1")
    private Long meetingId;

    @Schema(description = "Meeting code", example = "mtg_a3f8k2md9x")
    private String meetingCode;

    @Schema(description = "Share URL", example = "https://time2gather.org/meetings/mtg_a3f8k2md9x")
    private String shareUrl;

    /**
     * 도메인 모델로부터 DTO 생성
     */
    public static CreateMeetingResponse from(Meeting meeting) {
        return new CreateMeetingResponse(
                meeting.getId(),
                meeting.getMeetingCode(),
                "https://time2gather.org/meetings/" + meeting.getMeetingCode()
        );
    }
}



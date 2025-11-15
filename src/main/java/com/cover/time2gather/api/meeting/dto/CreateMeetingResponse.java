package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "모임 생성 응답")
public class CreateMeetingResponse {

    @Schema(description = "모임 ID", example = "1")
    private Long meetingId;

    @Schema(description = "모임 코드", example = "mtg_a3f8k2md9x")
    private String meetingCode;

    @Schema(description = "공유 URL", example = "https://time2gather.org/mtg_a3f8k2md9x")
    private String shareUrl;
}


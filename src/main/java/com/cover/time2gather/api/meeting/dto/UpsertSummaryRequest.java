package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Open AI 모임 요약 요청")
public class UpsertSummaryRequest {

    @Schema(description = "GPT 모델은 필수입니다",
            example = "gpt-4o-mini")
    private String model;

    @Schema(description = "요약할 모임 정보 텍스트",
            example = "모임 제목: 대학 동기 모임, 참석자: 김철수, 이영희...")
    private String input;

    @Schema(description = "모임 요약을 위한 지시문입니다",
            example = "위 제목, 사용자 시간 선택 정보를 바탕으로 모임 요약을 작성해주세요.")
    private String instructions;
}

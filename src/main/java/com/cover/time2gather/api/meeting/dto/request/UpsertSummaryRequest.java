package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Open AI 모임 요약 요청")
public class UpsertSummaryRequest {

    @Schema(description = "GPT 모델은 필수입니다",
            example = "gpt-4o-mini")
    private String model;

    @Schema(description = "대화 메시지 목록")
    private List<Message> messages;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        @Schema(description = "메시지 역할 (system, user, assistant)")
        private String role;

        @Schema(description = "메시지 내용")
        private String content;
    }

    /**
     * 기존 input, instructions를 OpenAI 표준 형식으로 변환하는 팩토리 메서드
     */
    public static UpsertSummaryRequest of(String model, String input, String instructions) {
        List<Message> messages = List.of(
                new Message("system", instructions),
                new Message("user", input)
        );
        return new UpsertSummaryRequest(model, messages);
    }
}

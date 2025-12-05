package com.cover.time2gather.api.meeting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "Open AI 요약 응답")
public class UpsertSummaryResponse {

    @Schema(description = "응답 ID")
    private String id;

    @Schema(description = "객체 타입")
    private String object;

    @Schema(description = "생성 시간")
    private Long created;

    @Schema(description = "모델명")
    private String model;

    @Schema(description = "응답 선택 목록")
    private List<Choice> choices;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Schema(description = "응답 선택")
    public static class Choice {
        @Schema(description = "인덱스")
        private Integer index;

        @Schema(description = "메시지 내용")
        private Message message;

        @Schema(description = "종료 이유")
        private String finish_reason;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Schema(description = "메시지")
    public static class Message {
        @Schema(description = "역할")
        private String role;

        @Schema(description = "내용")
        private String content;
    }

    public String getSummary() {
        if (choices != null && !choices.isEmpty()) {
            Choice firstChoice = choices.getFirst();
            if (firstChoice.getMessage() != null) {
                return firstChoice.getMessage().getContent();
            }
        }
        return "";
    }
}

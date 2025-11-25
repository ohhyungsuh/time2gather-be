package com.cover.time2gather.api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Open AI 요약 응답")
public class UpsertSummaryResponse {

    @Schema(description = "응답 출력 배열")
    private List<OutputItem> output;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputItem {
        @Schema(description = "컨텐츠 배열")
        private List<ContentItem> content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentItem {
        @Schema(description = "텍스트 내용")
        private String text;
    }

    public String getSummary() {
        if (output != null && !output.isEmpty()) {
            OutputItem firstOutput = output.getFirst();
            if (firstOutput.getContent() != null && !firstOutput.getContent().isEmpty()) {
                return firstOutput.getContent().getFirst().getText();
            }
        }
        return null;
    }
}

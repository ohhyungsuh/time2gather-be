package com.cover.time2gather.domain.meeting.client;

import com.cover.time2gather.domain.meeting.ReportData;
import com.cover.time2gather.infra.ai.AiChatClient;
import com.cover.time2gather.util.ReportInputTextBuilder;
import com.cover.time2gather.util.ResourceLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.cover.time2gather.domain.meeting.constants.ReportConstants.PROMPT_TEMPLATE_PATH;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAISummaryClient implements ReportSummaryClient {

    private final AiChatClient aiChatClient;

    @Override
    public String generateSummary(ReportData reportData) {
        try {
            String instructions = ResourceLoader.loadTextFile(PROMPT_TEMPLATE_PATH);
            String inputText = ReportInputTextBuilder.build(
                    reportData.meeting(),
                    reportData.selections(),
                    reportData.userMap(),
                    reportData.locations(),
                    reportData.locationSelections()
            );

            log.info("Sending summary request via {}. Meeting: {}",
                    aiChatClient.getProviderName(), reportData.meeting().getId());
            log.debug("Request body - Input length: {}, Instructions length: {}",
                    inputText.length(), instructions.length());

            String summary = aiChatClient.chat(instructions, inputText);

            if (summary == null || summary.isBlank()) {
                log.warn("AI returned empty summary for meeting: {}", reportData.meeting().getId());
                return "";
            }

            log.info("Successfully generated summary for meeting: {}. Summary length: {}",
                    reportData.meeting().getId(), summary.length());
            return summary;

        } catch (Exception e) {
            log.error("Failed to generate summary for meeting: {}. Error: {}",
                    reportData.meeting().getId(), e.getMessage(), e);
            return "";
        }
    }
}

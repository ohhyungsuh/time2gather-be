package com.cover.time2gather.domain.meeting.client;

import com.cover.time2gather.api.meeting.dto.request.UpsertSummaryRequest;
import com.cover.time2gather.api.meeting.dto.response.UpsertSummaryResponse;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.util.ReportInputTextBuilder;
import com.cover.time2gather.util.ResourceLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static com.cover.time2gather.domain.meeting.constants.ReportConstants.PROMPT_TEMPLATE_PATH;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAISummaryClient implements ReportSummaryClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model}")
    private String model;

    @Override
    public String generateSummary(Meeting meeting, List<MeetingUserSelection> selections, Map<Long, User> userMap) {
        try {
            String instructions = ResourceLoader.loadTextFile(PROMPT_TEMPLATE_PATH);
            String inputText = ReportInputTextBuilder.build(meeting, selections, userMap);
            UpsertSummaryRequest request = new UpsertSummaryRequest(model, inputText, instructions);

            log.info("Sending summary request to ChatGPT API. Model: {}, Meeting: {}", model, meeting.getId());
            log.debug("Request body - Input length: {}, Instructions length: {}",
                inputText.length(), instructions.length());

            // String으로 응답을 받아서 로깅
            String rawResponse = restClient
                    .post()
                    .uri("/responses")
                    .body(request)
                    .retrieve()
                    .body(String.class);

            log.info("Raw ChatGPT API response for meeting {}: {}", meeting.getId(), rawResponse);

            if (rawResponse == null || rawResponse.isBlank()) {
                log.error("ChatGPT API returned null or empty response for meeting: {}", meeting.getId());
                return "";
            }

            // ObjectMapper로 파싱
            UpsertSummaryResponse response = objectMapper.readValue(rawResponse, UpsertSummaryResponse.class);

            log.debug("Parsed ChatGPT API response: {}", response);

            String summary = response.getSummary();
            if (summary == null || summary.isBlank()) {
                log.warn("ChatGPT API returned empty summary for meeting: {}. Response object: {}",
                    meeting.getId(), response);
                return "";
            }

            log.info("Successfully generated summary for meeting: {}. Summary length: {}",
                meeting.getId(), summary.length());
            return summary;

        } catch (Exception e) {
            log.error("Failed to generate summary for meeting: {}. Error: {}",
                meeting.getId(), e.getMessage(), e);
            return "";
        }
    }
}

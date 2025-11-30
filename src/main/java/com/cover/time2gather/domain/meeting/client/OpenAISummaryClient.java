package com.cover.time2gather.domain.meeting.client;

import com.cover.time2gather.api.meeting.dto.request.UpsertSummaryRequest;
import com.cover.time2gather.api.meeting.dto.response.UpsertSummaryResponse;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.util.ReportInputTextBuilder;
import com.cover.time2gather.util.ResourceLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static com.cover.time2gather.domain.meeting.constants.ReportConstants.PROMPT_TEMPLATE_PATH;

@Component
@RequiredArgsConstructor
public class OpenAISummaryClient implements ReportSummaryClient {

    private final RestClient restClient;

    @Value("${openai.model}")
    private String model;

    @Override
    public String generateSummary(Meeting meeting, List<MeetingUserSelection> selections, Map<Long, User> userMap) {
        String instructions = ResourceLoader.loadTextFile(PROMPT_TEMPLATE_PATH);
        String inputText = ReportInputTextBuilder.build(meeting, selections, userMap);
        UpsertSummaryRequest request = new UpsertSummaryRequest(model, inputText, instructions);

        UpsertSummaryResponse response = restClient
                .post()
                .uri("/responses")
                .body(request)
                .retrieve()
                .body(UpsertSummaryResponse.class);

        if (response == null || response.getSummary() == null || response.getSummary().isBlank()) {
            return "";
        }

        return response.getSummary();
    }
}

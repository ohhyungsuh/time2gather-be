package com.cover.time2gather.infra.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI를 활용한 OpenAI ChatClient 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiChatClient implements AiChatClient {

    private static final String PROVIDER_NAME = "openai";

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public String chat(String systemPrompt, String userInput) {
        try {
            log.info("Sending chat request to OpenAI. System prompt length: {}, User input length: {}",
                    systemPrompt.length(), userInput.length());

            ChatClient chatClient = chatClientBuilder.build();

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userInput)
            ));

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            log.info("Received response from OpenAI. Response length: {}",
                    response != null ? response.length() : 0);
            log.debug("OpenAI response: {}", response);

            return response != null ? response : "";

        } catch (Exception e) {
            log.error("Failed to get response from OpenAI. Error: {}", e.getMessage(), e);
            throw new AiChatException("OpenAI API 호출 실패", e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}


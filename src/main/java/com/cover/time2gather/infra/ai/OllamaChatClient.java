package com.cover.time2gather.infra.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ollama (로컬 LLM) ChatClient 구현체
 *
 * 활성화하려면:
 * 1. build.gradle에 spring-ai-ollama-spring-boot-starter 의존성 추가
 * 2. application.yml에 ai.provider=ollama 설정
 * 3. Ollama 서버 실행 (기본: localhost:11434)
 *
 * 예시 설정:
 * spring:
 *   ai:
 *     ollama:
 *       base-url: http://localhost:11434
 *       chat:
 *         options:
 *           model: llama3.2
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
public class OllamaChatClient implements AiChatClient {

    private static final String PROVIDER_NAME = "ollama";

    // TODO: Ollama 실제 구현 시 아래 주석 해제
    // private final ChatClient.Builder chatClientBuilder;

    @Override
    public String chat(String systemPrompt, String userInput) {
        log.info("Ollama chat request. System prompt length: {}, User input length: {}",
                systemPrompt.length(), userInput.length());

        // TODO: 실제 Ollama 연동 구현
        // Spring AI Ollama starter 추가 후:
        //
        // ChatClient chatClient = chatClientBuilder.build();
        // Prompt prompt = new Prompt(List.of(
        //         new SystemMessage(systemPrompt),
        //         new UserMessage(userInput)
        // ));
        // return chatClient.prompt(prompt).call().content();

        throw new UnsupportedOperationException(
                "Ollama provider is not yet implemented. " +
                "Add spring-ai-ollama-spring-boot-starter dependency and implement this method."
        );
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}


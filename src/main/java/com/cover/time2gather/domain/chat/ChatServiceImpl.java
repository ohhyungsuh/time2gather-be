package com.cover.time2gather.domain.chat;

import com.cover.time2gather.api.chat.dto.ChatRequest;
import com.cover.time2gather.api.chat.dto.ChatResponse;
import com.cover.time2gather.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * ChatService 구현체 - Spring AI Tool Calling 기반
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String SYSTEM_PROMPT = """
        당신은 time2gather 앱의 일정 관리 도우미입니다.
        사용자의 미팅 일정을 조회하고 관련 정보를 제공합니다.
        
        규칙:
        - 정중한 존댓말을 사용하세요.
        - 미팅 정보를 조회할 때는 제공된 도구를 사용하세요.
        - 간결하고 명확하게 답변하세요.
        - 미팅 관련 질문이 아닌 경우, 일정 관련 도움만 제공할 수 있다고 안내하세요.
        
        현재 사용자 ID: %d
        """;

    private final ChatClient.Builder chatClientBuilder;
    private final MeetingQueryTools meetingQueryTools;

    @Override
    public ChatResponse chat(User user, ChatRequest request) {
        log.info("Processing chat request for user: {}, message: {}", user.getId(), request.message());

        String sessionId = resolveSessionId(request.sessionId());
        String systemPrompt = String.format(SYSTEM_PROMPT, user.getId());

        try {
            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.message())
                    .tools(meetingQueryTools)
                    .call()
                    .content();

            log.info("Chat response generated for session: {}", sessionId);
            return new ChatResponse(sessionId, response);

        } catch (Exception e) {
            log.error("Failed to process chat request", e);
            return new ChatResponse(sessionId, "죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }
}

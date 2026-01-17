package com.cover.time2gather.api.chat;

import com.cover.time2gather.api.chat.dto.ChatRequest;
import com.cover.time2gather.api.chat.dto.ChatResponse;
import com.cover.time2gather.config.JpaAuditingConfig;
import com.cover.time2gather.config.security.WithMockJwtUser;
import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.chat.ChatService;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.oauth.OidcProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = ChatController.class,
    excludeAutoConfiguration = JpaAuditingConfig.class
)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private OidcProviderRegistry oidcProviderRegistry;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .username("testUser")
            .provider(User.AuthProvider.KAKAO)
            .providerId("kakao_123")
            .build();

        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(testUser));
    }

    @Nested
    @DisplayName("POST /api/v1/chat")
    class Chat {

        @Test
        @WithMockJwtUser(userId = 1L, username = "testUser")
        @DisplayName("메시지를 보내면 챗봇 응답을 반환한다")
        void shouldReturnChatResponse() throws Exception {
            // Given
            ChatRequest request = new ChatRequest(null, "다음주 일정 뭐야?");
            ChatResponse response = new ChatResponse("session-123", "다음주에 2개의 미팅이 있습니다.");

            when(chatService.chat(any(User.class), any(ChatRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/v1/chat")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value("session-123"))
                .andExpect(jsonPath("$.data.message").value("다음주에 2개의 미팅이 있습니다."));
        }

        @Test
        @WithMockJwtUser(userId = 1L, username = "testUser")
        @DisplayName("기존 세션으로 메시지를 보내면 세션이 유지된다")
        void shouldMaintainSessionWithExistingSessionId() throws Exception {
            // Given
            ChatRequest request = new ChatRequest("session-123", "회식 몇명 참석해?");
            ChatResponse response = new ChatResponse("session-123", "팀 회식에 6명이 참석 가능합니다.");

            when(chatService.chat(any(User.class), any(ChatRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/v1/chat")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("session-123"));
        }

        @Test
        @WithMockJwtUser(userId = 1L, username = "testUser")
        @DisplayName("메시지가 비어있으면 400 Bad Request 반환")
        void shouldReturn400WhenMessageIsEmpty() throws Exception {
            // Given
            ChatRequest request = new ChatRequest(null, "");

            // When & Then
            mockMvc.perform(post("/api/v1/chat")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }
}

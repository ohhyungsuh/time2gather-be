// package com.cover.time2gather.integration;
//
// import com.cover.time2gather.api.chat.dto.ChatRequest;
// import com.cover.time2gather.config.security.WithMockJwtUser;
// import com.cover.time2gather.domain.user.User;
// import com.cover.time2gather.domain.user.UserRepository;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.context.TestConfiguration;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Import;
// import org.springframework.context.annotation.Primary;
// import org.springframework.http.MediaType;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.transaction.annotation.Transactional;
//
// import java.util.UUID;
//
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
// /**
//  * Chat API 통합 테스트
//  * 실제 Spring 컨텍스트에서 ChatController -> ChatService 흐름 검증
//  */
// @SpringBootTest
// @AutoConfigureMockMvc
// @Transactional
// @ActiveProfiles("test")
// @Import(ChatIntegrationTest.MockChatClientConfig.class)
// class ChatIntegrationTest {
//
//     @Autowired
//     private MockMvc mockMvc;
//
//     @Autowired
//     private ObjectMapper objectMapper;
//
//     @Autowired
//     private UserRepository userRepository;
//
//     private User testUser;
//
//     @TestConfiguration
//     static class MockChatClientConfig {
//         @Bean
//         @Primary
//         public ChatClient.Builder mockChatClientBuilder() {
//             ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
//             ChatClient mockChatClient = mock(ChatClient.class);
//             ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
//             ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);
//
//             when(mockBuilder.build()).thenReturn(mockChatClient);
//             when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.system(anyString())).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.tools(org.mockito.ArgumentMatchers.any(Object.class))).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.call()).thenReturn(mockCallResponse);
//             when(mockCallResponse.content()).thenReturn("테스트 응답입니다. 현재 미팅이 없습니다.");
//
//             return mockBuilder;
//         }
//     }
//
//     @BeforeEach
//     void setUp() {
//         // 각 테스트마다 유니크한 providerId 생성
//         String uniqueProviderId = "google_integration_" + UUID.randomUUID().toString();
//
//         testUser = User.builder()
//                 .username("integrationTestUser")
//                 .provider(User.AuthProvider.GOOGLE)
//                 .providerId(uniqueProviderId)
//                 .build();
//         testUser = userRepository.save(testUser);
//     }
//
//     @Nested
//     @DisplayName("POST /api/v1/chat 통합 테스트")
//     class ChatEndpoint {
//
//         @Test
//         @WithMockJwtUser(userId = 1L, username = "testUser")
//         @DisplayName("인증된 사용자가 메시지를 보내면 AI 응답을 받는다")
//         void shouldReturnAiResponseForAuthenticatedUser() throws Exception {
//             // Given
//             ChatRequest request = new ChatRequest(null, "내 미팅 목록 보여줘");
//
//             // When & Then
//             mockMvc.perform(post("/api/v1/chat")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andDo(print())
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.success").value(true))
//                     .andExpect(jsonPath("$.data.sessionId").exists())
//                     .andExpect(jsonPath("$.data.message").exists());
//         }
//
//         @Test
//         @WithMockJwtUser(userId = 1L, username = "testUser")
//         @DisplayName("빈 메시지로 요청하면 400 Bad Request 반환")
//         void shouldReturn400ForEmptyMessage() throws Exception {
//             // Given
//             ChatRequest request = new ChatRequest(null, "");
//
//             // When & Then
//             mockMvc.perform(post("/api/v1/chat")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andExpect(status().isBadRequest());
//         }
//
//         @Test
//         @WithMockJwtUser(userId = 1L, username = "testUser")
//         @DisplayName("세션 ID가 제공되면 동일한 세션 ID로 응답한다")
//         void shouldMaintainSessionId() throws Exception {
//             // Given
//             String sessionId = "test-session-12345";
//             ChatRequest request = new ChatRequest(sessionId, "안녕하세요");
//
//             // When & Then
//             mockMvc.perform(post("/api/v1/chat")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.success").value(true))
//                     .andExpect(jsonPath("$.data.sessionId").value(sessionId));
//         }
//     }
// }

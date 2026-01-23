// package com.cover.time2gather.domain.chat;
//
// import com.cover.time2gather.api.chat.dto.ChatRequest;
// import com.cover.time2gather.api.chat.dto.ChatResponse;
// import com.cover.time2gather.domain.user.User;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.ai.chat.client.ChatClient;
//
// import java.lang.reflect.Field;
//
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;
//
// @ExtendWith(MockitoExtension.class)
// class ChatServiceImplTest {
//
//     @Mock
//     private ChatClient.Builder chatClientBuilder;
//
//     @Mock
//     private MeetingQueryTools meetingQueryTools;
//
//     private ChatServiceImpl chatService;
//
//     @BeforeEach
//     void setUp() {
//         chatService = new ChatServiceImpl(chatClientBuilder, meetingQueryTools);
//     }
//
//     @Nested
//     @DisplayName("chat")
//     class Chat {
//
//         @Test
//         @DisplayName("사용자 메시지를 전달하면 AI 응답을 반환한다")
//         void shouldReturnAiResponse() {
//             // Given
//             User user = createTestUser(1L, "테스트유저");
//             ChatRequest request = new ChatRequest(null, "내 미팅 목록 보여줘");
//
//             ChatClient mockChatClient = mock(ChatClient.class);
//             ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
//             ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);
//
//             when(chatClientBuilder.build()).thenReturn(mockChatClient);
//             when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.system(anyString())).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.tools(any(Object.class))).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.call()).thenReturn(mockCallResponse);
//             when(mockCallResponse.content()).thenReturn("현재 등록된 미팅이 2개 있습니다.");
//
//             // When
//             ChatResponse response = chatService.chat(user, request);
//
//             // Then
//             assertThat(response).isNotNull();
//             assertThat(response.message()).isEqualTo("현재 등록된 미팅이 2개 있습니다.");
//         }
//
//         @Test
//         @DisplayName("sessionId가 없으면 새로운 sessionId를 생성하여 반환한다")
//         void shouldGenerateNewSessionIdWhenNotProvided() {
//             // Given
//             User user = createTestUser(1L, "테스트유저");
//             ChatRequest request = new ChatRequest(null, "안녕");
//
//             ChatClient mockChatClient = mock(ChatClient.class);
//             ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
//             ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);
//
//             when(chatClientBuilder.build()).thenReturn(mockChatClient);
//             when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.system(anyString())).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.tools(any(Object.class))).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.call()).thenReturn(mockCallResponse);
//             when(mockCallResponse.content()).thenReturn("안녕하세요!");
//
//             // When
//             ChatResponse response = chatService.chat(user, request);
//
//             // Then
//             assertThat(response.sessionId()).isNotNull();
//             assertThat(response.sessionId()).isNotBlank();
//         }
//
//         @Test
//         @DisplayName("sessionId가 제공되면 동일한 sessionId를 반환한다")
//         void shouldReturnSameSessionIdWhenProvided() {
//             // Given
//             User user = createTestUser(1L, "테스트유저");
//             String existingSessionId = "existing-session-123";
//             ChatRequest request = new ChatRequest(existingSessionId, "안녕");
//
//             ChatClient mockChatClient = mock(ChatClient.class);
//             ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
//             ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);
//
//             when(chatClientBuilder.build()).thenReturn(mockChatClient);
//             when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.system(anyString())).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.tools(any(Object.class))).thenReturn(mockRequestSpec);
//             when(mockRequestSpec.call()).thenReturn(mockCallResponse);
//             when(mockCallResponse.content()).thenReturn("안녕하세요!");
//
//             // When
//             ChatResponse response = chatService.chat(user, request);
//
//             // Then
//             assertThat(response.sessionId()).isEqualTo(existingSessionId);
//         }
//     }
//
//     private User createTestUser(Long id, String username) {
//         User user = User.builder()
//                 .username(username)
//                 .provider(User.AuthProvider.GOOGLE)
//                 .providerId("test-provider-id")
//                 .build();
//
//         // Reflection으로 id 설정
//         try {
//             Field idField = User.class.getDeclaredField("id");
//             idField.setAccessible(true);
//             idField.set(user, id);
//         } catch (Exception e) {
//             throw new RuntimeException(e);
//         }
//
//         return user;
//     }
// }

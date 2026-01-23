// package com.cover.time2gather.api.chat;
//
// import com.cover.time2gather.api.chat.dto.ChatRequest;
// import com.cover.time2gather.api.chat.dto.ChatResponse;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
//
// import java.util.List;
//
// import static org.assertj.core.api.Assertions.assertThat;
//
// class ChatDtoTest {
//
//     @Nested
//     @DisplayName("ChatRequest")
//     class ChatRequestTest {
//
//         @Test
//         @DisplayName("sessionId와 message로 생성할 수 있다")
//         void shouldCreateWithSessionIdAndMessage() {
//             // Given
//             String sessionId = "session-123";
//             String message = "다음주 일정 뭐야?";
//
//             // When
//             ChatRequest request = new ChatRequest(sessionId, message);
//
//             // Then
//             assertThat(request.sessionId()).isEqualTo(sessionId);
//             assertThat(request.message()).isEqualTo(message);
//         }
//
//         @Test
//         @DisplayName("sessionId가 null일 수 있다 (새 세션 생성)")
//         void shouldAllowNullSessionId() {
//             // Given
//             String message = "다음주 일정 뭐야?";
//
//             // When
//             ChatRequest request = new ChatRequest(null, message);
//
//             // Then
//             assertThat(request.sessionId()).isNull();
//             assertThat(request.message()).isEqualTo(message);
//         }
//     }
//
//     @Nested
//     @DisplayName("ChatResponse")
//     class ChatResponseTest {
//
//         @Test
//         @DisplayName("sessionId와 message로 생성할 수 있다")
//         void shouldCreateWithSessionIdAndMessage() {
//             // Given
//             String sessionId = "session-123";
//             String message = "다음주에 2개의 미팅이 있습니다.";
//
//             // When
//             ChatResponse response = new ChatResponse(sessionId, message);
//
//             // Then
//             assertThat(response.sessionId()).isEqualTo(sessionId);
//             assertThat(response.message()).isEqualTo(message);
//         }
//     }
// }

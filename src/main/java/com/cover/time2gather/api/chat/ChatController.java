package com.cover.time2gather.api.chat;

import com.cover.time2gather.api.chat.dto.ChatRequest;
import com.cover.time2gather.api.chat.dto.ChatResponse;
import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.config.security.CurrentUser;
import com.cover.time2gather.domain.chat.ChatService;
import com.cover.time2gather.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ApiResponse<ChatResponse> chat(
        @CurrentUser User user,
        @RequestBody @Valid ChatRequest request
    ) {
        ChatResponse response = chatService.chat(user, request);
        return ApiResponse.success(response);
    }
}

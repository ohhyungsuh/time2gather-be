package com.cover.time2gather.domain.chat;

import com.cover.time2gather.api.chat.dto.ChatRequest;
import com.cover.time2gather.api.chat.dto.ChatResponse;
import com.cover.time2gather.domain.user.User;

public interface ChatService {
    ChatResponse chat(User user, ChatRequest request);
}

package com.cover.time2gather.api.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
    String sessionId,
    @NotBlank(message = "메시지를 입력해주세요")
    String message
) {}

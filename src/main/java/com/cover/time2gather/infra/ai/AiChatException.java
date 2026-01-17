package com.cover.time2gather.infra.ai;

/**
 * AI Chat 관련 예외
 */
public class AiChatException extends RuntimeException {

    public AiChatException(String message) {
        super(message);
    }

    public AiChatException(String message, Throwable cause) {
        super(message, cause);
    }
}


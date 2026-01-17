package com.cover.time2gather.infra.ai;

import lombok.Getter;

/**
 * 지원하는 AI Provider 타입
 */
@Getter
public enum AiProviderType {

    OPENAI("openai", "OpenAI GPT"),
    ANTHROPIC("anthropic", "Anthropic Claude"),
    OLLAMA("ollama", "Ollama (Local LLM)");

    private final String code;
    private final String description;

    AiProviderType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AiProviderType fromCode(String code) {
        for (AiProviderType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AI provider code: " + code);
    }
}


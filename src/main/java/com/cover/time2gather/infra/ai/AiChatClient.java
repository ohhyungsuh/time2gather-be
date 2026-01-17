package com.cover.time2gather.infra.ai;

/**
 * AI Chat Provider 추상화 인터페이스
 * OpenAI, Anthropic, Ollama 등 다양한 LLM Provider를 지원하기 위한 추상화 레이어
 */
public interface AiChatClient {

    /**
     * 시스템 프롬프트와 사용자 입력을 받아 AI 응답을 생성
     *
     * @param systemPrompt AI의 역할과 동작 방식을 정의하는 시스템 프롬프트
     * @param userInput 사용자가 입력한 내용
     * @return AI가 생성한 응답 텍스트
     */
    String chat(String systemPrompt, String userInput);

    /**
     * Provider 이름 반환
     *
     * @return Provider 이름 (예: "openai", "anthropic", "ollama")
     */
    String getProviderName();
}


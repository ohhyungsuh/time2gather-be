package com.cover.time2gather.config;

import com.cover.time2gather.infra.ai.AiChatClient;
import com.cover.time2gather.infra.ai.AiProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI Provider 설정
 * Provider를 쉽게 전환할 수 있도록 추상화된 설정
 */
@Slf4j
@Configuration
public class AiConfig {

    @Value("${ai.provider:openai}")
    private String activeProvider;

    /**
     * 활성화된 Provider에 따라 AiChatClient Bean 선택
     */
    @Bean
    @Primary
    public AiChatClient aiChatClient(List<AiChatClient> clients) {
        Map<String, AiChatClient> clientMap = clients.stream()
                .collect(Collectors.toMap(AiChatClient::getProviderName, Function.identity()));

        AiProviderType providerType = AiProviderType.fromCode(activeProvider);
        AiChatClient selectedClient = clientMap.get(providerType.getCode());

        if (selectedClient == null) {
            log.warn("Requested AI provider '{}' not found, falling back to first available", activeProvider);
            selectedClient = clients.get(0);
        }

        log.info("Using AI provider: {} ({})", selectedClient.getProviderName(), providerType.getDescription());
        return selectedClient;
    }
}


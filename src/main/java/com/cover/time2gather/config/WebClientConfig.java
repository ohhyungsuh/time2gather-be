package com.cover.time2gather.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class WebClientConfig {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.api-base}")
    private String apiBase;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(apiBase)
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .defaultHeader(AUTHORIZATION, BEARER_PREFIX + apiKey)
                .build();
    }
}

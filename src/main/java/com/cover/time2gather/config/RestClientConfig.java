package com.cover.time2gather.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Configuration
public class RestClientConfig {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.api-base}")
    private String apiBase;

    @Bean
    public RestClient restClient() {
        ClientHttpRequestInterceptor loggingInterceptor = (request, body, execution) -> {
            log.info("Request URI: {} {}", request.getMethod(), request.getURI());
            log.debug("Request Headers: {}", request.getHeaders());
            log.debug("Request Body: {}", new String(body));

            var response = execution.execute(request, body);

            log.info("Response Status: {}", response.getStatusCode());
            log.debug("Response Headers: {}", response.getHeaders());

            return response;
        };

        return RestClient.builder()
                .baseUrl(apiBase)
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .defaultHeader(AUTHORIZATION, BEARER_PREFIX + apiKey)
                .requestInterceptor(loggingInterceptor)
                .build();
    }
}

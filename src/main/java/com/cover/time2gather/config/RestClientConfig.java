package com.cover.time2gather.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Configuration
public class RestClientConfig {

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
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .requestInterceptor(loggingInterceptor)
                .build();
    }
}

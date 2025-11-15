package com.cover.time2gather.config;

import com.cover.time2gather.domain.auth.oidc.OidcProviderStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@TestConfiguration
public class TestOAuthConfig {

    @Bean
    @Primary
    public RestTemplate testRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Primary
    public List<OidcProviderStrategy> testOidcProviders() {
        // Return empty list, individual tests will mock as needed
        return Collections.emptyList();
    }
}


package com.cover.time2gather.config;

import com.cover.time2gather.infra.oauth.GoogleOidcProvider;
import com.cover.time2gather.infra.oauth.KakaoOidcProvider;
import com.cover.time2gather.infra.oauth.OidcProviderStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class OAuthConfig {

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public List<OidcProviderStrategy> oidcProviders(RestTemplate restTemplate) {
        return List.of(
                new KakaoOidcProvider(restTemplate, kakaoClientId, kakaoClientSecret),
                new GoogleOidcProvider(restTemplate, googleClientId, googleClientSecret)
        );
    }
}


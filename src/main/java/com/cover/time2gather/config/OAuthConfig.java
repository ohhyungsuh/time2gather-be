package com.cover.time2gather.config;

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

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public List<OidcProviderStrategy> oidcProviders(RestTemplate restTemplate) {
        return List.of(
                new KakaoOidcProvider(restTemplate, kakaoClientId, kakaoClientSecret)
                // 추후 Google Provider 등 추가 가능
        );
    }
}


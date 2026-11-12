package com.cover.time2gather.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class AdminGoogleConfig {
    @Bean
    public JwtDecoder adminGoogleJwtDecoder() {
        // Keys are fetched lazily and cached. Nimbus checks RS256 signatures and timestamps.
        return NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
    }
}

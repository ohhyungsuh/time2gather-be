package com.cover.time2gather.config;

import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:3600000}") // Default: 1 hour
    private long jwtExpirationMs;

    @Bean
    public JwtTokenService jwtTokenService() {
        return new JwtTokenService(jwtSecret, jwtExpirationMs);
    }
}


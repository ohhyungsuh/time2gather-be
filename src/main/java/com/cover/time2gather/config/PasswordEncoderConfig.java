package com.cover.time2gather.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password 암호화 설정
 * 
 * DelegatingPasswordEncoder를 사용하여 다양한 인코딩 형식 지원:
 * - {bcrypt} - BCrypt (기본값)
 * - {noop} - 평문 (OAuth2 클라이언트 시크릿 등에 사용)
 * - {pbkdf2}, {scrypt}, {argon2} 등
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // createDelegatingPasswordEncoder()는 기본값으로 bcrypt를 사용하면서
        // {noop}, {bcrypt} 등의 prefix를 인식합니다.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}


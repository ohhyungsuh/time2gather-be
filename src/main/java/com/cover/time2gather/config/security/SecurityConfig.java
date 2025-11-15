package com.cover.time2gather.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/meetings/*/auth/**").permitAll() // Anonymous login
                        .requestMatchers("/api/v1/meetings/*").permitAll() // Public meeting view
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger UI
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",           // 로컬 개발 환경 (모든 포트)
                "http://127.0.0.1:*",           // 로컬 개발 환경 (모든 포트)
                "http://frontend:*",            // Docker 컨테이너 서비스명
                "http://frontend",              // Docker 컨테이너 서비스명 (기본 포트)
                "https://time2gather.org",      // 프로덕션
                "https://www.time2gather.org",  // 프로덕션 (www)
                "https://*.time2gather.org"     // 프로덕션 서브도메인
        ));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        
        // 허용할 헤더 (모든 헤더 허용)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용 (쿠키 등)
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);
        
        // 노출할 헤더 (클라이언트에서 접근 가능한 헤더)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Set-Cookie",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

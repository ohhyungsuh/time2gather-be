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

    // CORS 설정 상수
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://frontend:*",
            "http://frontend",
            "https://time2gather.org",
            "https://www.time2gather.org",
            "https://*.time2gather.org"
    );

    private static final List<String> ALLOWED_METHODS = Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
    );

    private static final List<String> EXPOSED_HEADERS = Arrays.asList(
            "Authorization",
            "Set-Cookie",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
    );

    private static final long CORS_MAX_AGE_SECONDS = 3600L;

    // API 엔드포인트 상수
    private static final String AUTH_API_PATTERN = "/api/v1/auth/**";
    private static final String MEETING_AUTH_PATTERN = "/api/v1/meetings/*/auth/**";
    private static final String MEETING_PUBLIC_PATTERN = "/api/v1/meetings/*";
    private static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";
    private static final String API_DOCS_PATTERN = "/v3/api-docs/**";
    private static final String CORS_ALL_PATHS = "/**";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_API_PATTERN).permitAll()
                        .requestMatchers(MEETING_AUTH_PATTERN).permitAll() // Anonymous login
                        .requestMatchers(MEETING_PUBLIC_PATTERN).permitAll() // Public meeting view
                        .requestMatchers(SWAGGER_UI_PATTERN, API_DOCS_PATTERN).permitAll() // Swagger UI
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정
        configuration.setAllowedOriginPatterns(ALLOWED_ORIGINS);

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(ALLOWED_METHODS);

        // 허용할 헤더 (모든 헤더 허용)
        configuration.setAllowedHeaders(List.of("*"));

        // 인증 정보 포함 허용 (쿠키 등)
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(CORS_MAX_AGE_SECONDS);

        // 노출할 헤더 (클라이언트에서 접근 가능한 헤더)
        configuration.setExposedHeaders(EXPOSED_HEADERS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CORS_ALL_PATHS, configuration);

        return source;
    }
}

package com.cover.time2gather.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // CORS 설정 상수
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "https://time2gather.org",
			"http://localhost:3000",
			"https://localhost:3000",
            "https://www.time2gather.org",
            "https://*.time2gather.org",
            // PlayMCP (카카오)
            "https://playmcp.kakao.com",
            "https://*.playmcp.kakao.com"
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

    // Public endpoints (인증 불필요)
    private static final String MEETING_DETAIL_PATTERN = "/api/v1/meetings/*"; // GET /meetings/{code}
    private static final String MEETING_REPORT_PATTERN = "/api/v1/meetings/*/report"; // GET /meetings/{code}/report
    private static final String MEETING_EXPORT_PATTERN = "/api/v1/meetings/*/export"; // POST /meetings/{code}/export

    private static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";
    private static final String API_DOCS_PATTERN = "/v3/api-docs/**";
    private static final String ACTUATOR_PATTERN = "/actuator/**";
    private static final String HEALTH_CHECK_PATTERN = "/health";
    private static final String ROOT_PATTERN = "/";
    private static final String FAVICON_PATTERN = "/favicon.ico";
    private static final String CORS_ALL_PATHS = "/**";

    // MCP Server endpoints
    private static final String MCP_PATTERN = "/mcp/**";
    private static final String SSE_PATTERN = "/sse";

    /**
     * OAuth2 로그인용 Security Filter Chain
     * 폼 로그인 및 세션 기반 인증 (OAuth2 Authorization Server용)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain loginSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/login", "/login/**", "/oauth2/**", "/userinfo", "/.well-known/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/login/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/.well-known/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * API용 Security Filter Chain
     * JWT 기반 Stateless 인증
     */
    @Bean
    @Order(3)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("OPTIONS", "/**").permitAll() // Allow all OPTIONS requests for CORS preflight
                        .requestMatchers(FAVICON_PATTERN).permitAll() // Favicon
                        .requestMatchers(HEALTH_CHECK_PATTERN).permitAll() // Health check for AWS
                        .requestMatchers(ROOT_PATTERN).permitAll() // Root endpoint
                        .requestMatchers(ACTUATOR_PATTERN).permitAll() // Actuator endpoints
                        .requestMatchers(SWAGGER_UI_PATTERN, API_DOCS_PATTERN).permitAll() // Swagger UI

                        // MCP Server endpoints (for PlayMCP integration)
                        .requestMatchers(MCP_PATTERN).permitAll()
                        .requestMatchers(SSE_PATTERN).permitAll()

                        // Auth endpoints
                        .requestMatchers(AUTH_API_PATTERN).permitAll()
                        .requestMatchers(MEETING_AUTH_PATTERN).permitAll() // Anonymous login

                        // Public meeting endpoints (인증 불필요)
                        .requestMatchers("GET", MEETING_DETAIL_PATTERN).permitAll() // GET /meetings/{code}
                        .requestMatchers("GET", MEETING_REPORT_PATTERN).permitAll() // GET /meetings/{code}/report
                        .requestMatchers("POST", MEETING_EXPORT_PATTERN).permitAll() // POST /meetings/{code}/export

                        // All other requests require authentication (인증 필요)
                        // - POST /meetings (모임 생성)
                        // - GET /meetings/{code}/selections (내 선택 조회)
                        // - PUT /meetings/{code}/selections (시간 선택/수정)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(CORS_MAX_AGE_SECONDS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CORS_ALL_PATHS, configuration);

        return source;
    }
}

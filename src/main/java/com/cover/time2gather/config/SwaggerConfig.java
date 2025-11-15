package com.cover.time2gather.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger/OpenAPI 설정
 */
@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url("http://localhost:" + serverPort).description("Local Server"));
        servers.add(new Server().url("http://localhost:8080").description("Local Server (Default)"));
        servers.add(new Server().url("https://api.time2gather.org").description("Production Server"));
        
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("accessToken")
                                .description("JWT Token in HttpOnly Cookie"))
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Token in Authorization Header")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("cookieAuth")
                        .addList("bearerAuth"));
    }

    private Info apiInfo() {
        return new Info()
                .title("Time2Gather API")
                .description("일정 조율 서비스 API 문서\n\n" +
                        "### 인증 방법\n" +
                        "1. **Cookie 인증** (기본): 로그인 후 자동으로 설정되는 HttpOnly Cookie 사용\n" +
                        "2. **Bearer 토큰**: Authorization 헤더에 `Bearer {token}` 형식으로 전달\n\n" +
                        "### CORS 설정\n" +
                        "- 로컬 개발: http://localhost:* (모든 포트 허용)\n" +
                        "- 프로덕션: https://time2gather.org")
                .version("1.0.0")
                .contact(new Contact()
                        .name("Time2Gather Team")
                        .email("contact@time2gather.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Swagger UI를 위한 추가 CORS 설정
     * (SecurityConfig의 CORS 설정과 함께 작동)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

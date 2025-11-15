package com.cover.time2gather.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private static final String SECRET_KEY = "test-secret-key-for-jwt-token-generation-must-be-long-enough-256-bits";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(SECRET_KEY, EXPIRATION_MS);
    }

    @Test
    void shouldGenerateJwtToken() {
        // Given
        Long userId = 1L;
        String username = "testuser";

        // When
        String token = jwtTokenService.generateToken(userId, username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).contains(".");  // JWT format: header.payload.signature
    }

    @Test
    void shouldValidateValidToken() {
        // Given
        String token = jwtTokenService.generateToken(1L, "testuser");

        // When
        boolean isValid = jwtTokenService.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldNotValidateInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtTokenService.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldExtractUserIdFromToken() {
        // Given
        Long userId = 123L;
        String token = jwtTokenService.generateToken(userId, "testuser");

        // When
        Long extractedUserId = jwtTokenService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void shouldExtractUsernameFromToken() {
        // Given
        String username = "john_doe";
        String token = jwtTokenService.generateToken(1L, username);

        // When
        String extractedUsername = jwtTokenService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void shouldExtractAllClaimsFromToken() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String token = jwtTokenService.generateToken(userId, username);

        // When
        Claims claims = jwtTokenService.extractClaims(token);

        // Then
        assertThat(claims.get("userId", Long.class)).isEqualTo(userId);
        assertThat(claims.getSubject()).isEqualTo(username);
    }
}


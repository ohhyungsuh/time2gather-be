package com.cover.time2gather.config.security;

import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenService);
    }

    @Test
    void shouldAuthenticateWithValidJwtCookie() throws ServletException, IOException {
        // Given
        String jwtToken = "valid-jwt-token";
        Cookie cookie = new Cookie("accessToken", jwtToken);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(jwtTokenService.validateToken(jwtToken)).thenReturn(true);
        when(jwtTokenService.extractUserId(jwtToken)).thenReturn(1L);
        when(jwtTokenService.extractUsername(jwtToken)).thenReturn("testuser");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("testuser");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWithInvalidToken() throws ServletException, IOException {
        // Given
        String jwtToken = "invalid-jwt-token";
        Cookie cookie = new Cookie("accessToken", jwtToken);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(jwtTokenService.validateToken(jwtToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueFilterChainWhenNoCookie() throws ServletException, IOException {
        // Given
        when(request.getCookies()).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }
}


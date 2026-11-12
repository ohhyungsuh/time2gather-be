package com.cover.time2gather.config.security;

import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.user.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 쿠키를 검증하고 인증 정보를 SecurityContext에 설정하는 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AdminIdentityService adminIdentityService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        if (path == null) path = "";
        if (path.equals("/api/v1/admin") || path.startsWith("/api/v1/admin/")) {
            // Never accept an anonymous/legacy ADMIN cookie as admin authentication.
            SecurityContextHolder.clearContext();
            response.setHeader("Cache-Control", "no-store");
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                try {
                    SecurityContextHolder.getContext().setAuthentication(
                            adminIdentityService.authenticate(authorization.substring(7)));
                } catch (org.springframework.security.core.AuthenticationException ex) {
                    // The security entry point returns a generic 401, without disclosing the allowlist.
                }
            }
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwtToken = extractTokenFromCookie(request);

            if (jwtToken != null && jwtTokenService.validateToken(jwtToken)) {
                Long userId = jwtTokenService.extractUserId(jwtToken);
                String username = jwtTokenService.extractUsername(jwtToken);
                UserRole role = jwtTokenService.extractRole(jwtToken);

                JwtAuthentication authentication = new JwtAuthentication(userId, username, role);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.debug("JWT authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            // Keep accepting the legacy cookie during the cutover while issuing
            // Firebase Hosting's reserved __session cookie for new sessions.
            if ("__session".equals(cookie.getName()) || "accessToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}

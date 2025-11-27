package com.cover.time2gather.api.auth.vo;

import jakarta.servlet.http.Cookie;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * JWT 토큰을 담는 HTTP Cookie Value Object
 * - 쿠키 설정의 불변성과 일관성 보장
 * - 보안 설정(HttpOnly, Secure, SameSite) 캡슐화
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtTokenCookie {

    private static final String COOKIE_NAME = "accessToken";
    private static final int MAX_AGE_SECONDS = 3600; // 1시간
    private static final String COOKIE_PATH = "/";
    private static final boolean HTTP_ONLY = true;
    private static final boolean SECURE = true;
    private static final String SAME_SITE = "None"; // cross-site 요청 허용 (Secure=true 필수)

    private final Cookie cookie;

    public static JwtTokenCookie from(String jwtToken) {
        Cookie cookie = new Cookie(COOKIE_NAME, jwtToken);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(MAX_AGE_SECONDS);

        return new JwtTokenCookie(cookie);
    }

    public static JwtTokenCookie expired() {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(0);
        return new JwtTokenCookie(cookie);
    }

    /**
     * SameSite=None을 포함한 Set-Cookie 헤더 값 생성
     * Jakarta Servlet의 Cookie 클래스는 SameSite를 직접 지원하지 않으므로
     * 수동으로 헤더를 구성해야 함
     */
    public String toSetCookieHeader() {
        StringBuilder header = new StringBuilder();
        header.append(COOKIE_NAME).append("=").append(cookie.getValue()).append("; ");
        header.append("Path=").append(COOKIE_PATH).append("; ");
        header.append("Max-Age=").append(cookie.getMaxAge()).append("; ");

        if (HTTP_ONLY) {
            header.append("HttpOnly; ");
        }
        if (SECURE) {
            header.append("Secure; ");
        }
        header.append("SameSite=").append(SAME_SITE);

        return header.toString();
    }
}


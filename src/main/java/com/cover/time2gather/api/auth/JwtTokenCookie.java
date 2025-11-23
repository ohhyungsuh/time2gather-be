package com.cover.time2gather.api.auth;

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
}


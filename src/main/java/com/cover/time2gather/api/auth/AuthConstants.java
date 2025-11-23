package com.cover.time2gather.api.auth;

/**
 * 인증 관련 상수 정의
 */
public final class AuthConstants {

    private AuthConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    /**
     * JWT 토큰 쿠키 관련 상수
     */
    public static final class Cookie {
        public static final String ACCESS_TOKEN_NAME = "accessToken";
        public static final String COOKIE_PATH = "/";
        public static final int MAX_AGE_SECONDS = 3600; // 1 hour
        public static final boolean HTTP_ONLY = true;
        public static final boolean SECURE = true;

        private Cookie() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }
}


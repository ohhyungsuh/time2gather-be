package com.cover.time2gather.config.security;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.util.MessageProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 사용자의 요청에 대한 401 응답 처리
 * Accept-Language 헤더 기반으로 다국어 에러 메시지를 반환합니다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(CONTENT_TYPE_JSON);

        String message = MessageProvider.getMessage(ErrorCode.AUTH_REQUIRED_LOGIN.getMessageKey());
        ApiResponse<Void> errorResponse = ApiResponse.error(message);
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }
}

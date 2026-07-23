package com.cover.time2gather.config.security;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.util.MessageProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(CONTENT_TYPE_JSON);

        String message = MessageProvider.getMessage(ErrorCode.ACCESS_DENIED.getMessageKey());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }
}

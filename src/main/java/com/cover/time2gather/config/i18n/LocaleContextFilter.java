package com.cover.time2gather.config.i18n;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.Locale;

/**
 * Accept-Language 헤더를 파싱하여 LocaleContextHolder에 설정하는 필터
 * ThreadLocal 기반으로 현재 요청의 Locale을 관리합니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocaleContextFilter extends OncePerRequestFilter {

    private final LocaleResolver localeResolver;

    public LocaleContextFilter(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Locale locale = localeResolver.resolveLocale(request);
        LocaleContextHolder.setLocale(locale);

        try {
            filterChain.doFilter(request, response);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}

package com.cover.time2gather.config.security;

import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 인증된 사용자 정보를 컨트롤러 메서드의 파라미터로 자동 주입
 */
@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        if (!(authentication instanceof JwtAuthentication jwtAuth)) {
            throw new IllegalStateException("JWT 인증 정보가 아닙니다.");
        }

        Long userId = jwtAuth.getUserId();

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }
}


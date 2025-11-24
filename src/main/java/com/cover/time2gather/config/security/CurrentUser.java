package com.cover.time2gather.config.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * 현재 인증된 사용자 정보를 주입받기 위한 애노테이션
 * @AuthenticationPrincipal을 감싼 메타 애노테이션
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "userId")
public @interface CurrentUser {
}


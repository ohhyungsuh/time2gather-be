package com.cover.time2gather.config.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * WithMockJwtUser 어노테이션을 처리하여 SecurityContext를 생성
 */
public class WithMockJwtUserSecurityContextFactory implements WithSecurityContextFactory<WithMockJwtUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockJwtUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        JwtAuthentication authentication = new JwtAuthentication(
            annotation.userId(),
            annotation.username()
        );
        
        context.setAuthentication(authentication);
        return context;
    }
}

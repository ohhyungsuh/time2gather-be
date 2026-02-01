package com.cover.time2gather.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * CustomUserPrincipal을 감싸는 OAuth2User 구현체
 * OAuth2 Authorization Server와 OAuth2 Client 간의 호환성을 위해 사용
 */
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final CustomUserPrincipal userPrincipal;
    private final Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userPrincipal.getAuthorities();
    }

    @Override
    public String getName() {
        return userPrincipal.getEmail();
    }

    public CustomUserPrincipal getUserPrincipal() {
        return userPrincipal;
    }

    public Long getUserId() {
        return userPrincipal.getUserId();
    }

    public String getEmail() {
        return userPrincipal.getEmail();
    }

    public String getUsername() {
        return userPrincipal.getUsername();
    }
}

package com.cover.time2gather.config.security;

import com.cover.time2gather.domain.user.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * JWT 기반 인증 정보를 담는 Authentication 구현체
 * SecurityContext에 저장되어 @AuthenticationPrincipal로 주입 가능
 */
public record JwtAuthentication(
        Long userId,
        String username,
        UserRole role
) implements Authentication {

    public JwtAuthentication(Long userId, String username) {
        this(userId, username, UserRole.USER);
    }

    public JwtAuthentication {
        role = role != null ? role : UserRole.USER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.asAuthority()));
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new IllegalArgumentException("Cannot change authentication status");
    }

    @Override
    public String getName() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }
}

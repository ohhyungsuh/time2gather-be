package com.cover.time2gather.config.security;

import com.cover.time2gather.domain.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetails implementation that includes userId.
 * Used for OAuth2 Authorization Server authentication.
 */
@Getter
public class CustomUserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final String displayName;
    private final String principalName; // Non-null identifier for Spring Security
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomUserPrincipal(Long userId, String email, String password, String displayName,
                                String principalName, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.principalName = principalName;
        this.authorities = authorities;
    }

    public static CustomUserPrincipal from(User user) {
        // principalName must be non-null - use email if available, otherwise use providerId or id
        String principalName = user.getEmail();
        if (principalName == null || principalName.isEmpty()) {
            principalName = user.getProviderId();
        }
        if (principalName == null || principalName.isEmpty()) {
            principalName = String.valueOf(user.getId());
        }
        
        return new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword() != null ? user.getPassword() : "",
                user.getUsername(),
                principalName,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // Spring Security uses this as the principal identifier - must be non-empty
        return principalName;
    }
    
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

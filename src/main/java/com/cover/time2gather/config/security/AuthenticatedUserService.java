package com.cover.time2gather.config.security;

import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service to extract authenticated user information from SecurityContext.
 * Supports both form login (CustomUserPrincipal) and OAuth2 JWT token authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    /**
     * Get the currently authenticated user's ID.
     * 
     * @return Optional containing userId if authenticated, empty otherwise
     */
    public Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        // Case 1: Form login with CustomUserPrincipal
        if (principal instanceof CustomUserPrincipal customUser) {
            return Optional.of(customUser.getUserId());
        }

        // Case 2: OAuth2 JWT token (from MCP tools called via PlayMCP)
        if (principal instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("sub");
            if (email != null) {
                return userRepository.findByEmail(email)
                        .map(User::getId);
            }
        }

        // Case 3: String principal (email from JWT)
        if (principal instanceof String email) {
            return userRepository.findByEmail(email)
                    .map(User::getId);
        }

        return Optional.empty();
    }

    /**
     * Get the currently authenticated user's ID.
     * 
     * @return userId
     * @throws IllegalStateException if user is not authenticated
     */
    public Long getRequiredCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User is not authenticated"));
    }

    /**
     * Get the currently authenticated user entity.
     * 
     * @return Optional containing User if authenticated, empty otherwise
     */
    public Optional<User> getCurrentUser() {
        return getCurrentUserId()
                .flatMap(userRepository::findById);
    }

    /**
     * Get the currently authenticated user entity.
     * 
     * @return User entity
     * @throws IllegalStateException if user is not authenticated
     */
    public User getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("User is not authenticated"));
    }
}

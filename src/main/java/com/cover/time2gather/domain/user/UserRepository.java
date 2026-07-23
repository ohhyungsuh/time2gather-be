package com.cover.time2gather.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    long countByProvider(User.AuthProvider provider);

    long countByProviderNot(User.AuthProvider provider);

    long countByRole(UserRole role);

    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username,
            String email,
            Pageable pageable
    );
}

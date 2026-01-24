package com.cover.time2gather.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}


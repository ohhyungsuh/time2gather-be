package com.cover.time2gather.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/** Persistence port implemented by JPA locally and Firestore in production. */
public interface UserRepository {

    <S extends User> S save(S entity);

    Optional<User> findById(Long id);

    boolean existsById(Long id);

    List<User> findAllById(Iterable<Long> ids);

    List<User> findAll();

    void deleteAll();

    Page<User> findAll(Pageable pageable);

    long count();

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

package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.domain.user.UserRole;
import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@Profile("firestore")
public class FirestoreUserRepository extends FirestoreRepositorySupport<User> implements UserRepository {

    public FirestoreUserRepository(Firestore firestore) {
        super(firestore, "users", User.class);
    }

    @Override
    public <S extends User> S save(S entity) {
        return saveEntity(entity);
    }

    @Override
    public Optional<User> findById(Long id) {
        return findEntityById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return entityExists(id);
    }

    @Override
    public List<User> findAllById(Iterable<Long> ids) {
        return findEntitiesByIds(ids);
    }

    @Override
    public List<User> findAll() {
        return allEntities();
    }

    @Override
    public void deleteAll() {
        deleteAllEntities();
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return page(newestFirst(allEntities()), pageable);
    }

    @Override
    public long count() {
        return countAll();
    }

    @Override
    public Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId) {
        List<User> matches = queryEquals("provider", provider.name(), "providerId", providerId);
        return matches.stream().findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return firstEquals("username", username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return firstEquals("email", email);
    }

    @Override
    public long countByProvider(User.AuthProvider provider) {
        return countMatching(user -> user.getProvider() == provider);
    }

    @Override
    public long countByProviderNot(User.AuthProvider provider) {
        return countMatching(user -> user.getProvider() != provider);
    }

    @Override
    public long countByRole(UserRole role) {
        return countMatching(user -> user.getRole() == role);
    }

    @Override
    public Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, Pageable pageable) {
        String usernameQuery = normalize(username);
        String emailQuery = normalize(email);
        List<User> matches = newestFirst(allEntities().stream()
                .filter(user -> normalize(user.getUsername()).contains(usernameQuery)
                        || normalize(user.getEmail()).contains(emailQuery))
                .toList());
        return page(matches, pageable);
    }

    private List<User> newestFirst(List<User> users) {
        return sorted(users, Comparator.comparing(User::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

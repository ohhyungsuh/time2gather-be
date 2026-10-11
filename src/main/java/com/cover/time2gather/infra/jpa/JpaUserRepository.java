package com.cover.time2gather.infra.jpa;

import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!firestore")
public interface JpaUserRepository extends UserRepository, JpaRepository<User, Long> {
}

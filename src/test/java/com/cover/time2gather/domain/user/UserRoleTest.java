package com.cover.time2gather.domain.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleTest {

    @Test
    void shouldDefaultNewUsersToUserRole() {
        User user = User.builder()
                .username("new-user")
                .provider(User.AuthProvider.GOOGLE)
                .providerId("google-id")
                .build();

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void shouldKeepExplicitAdminRole() {
        User user = User.builder()
                .username("admin")
                .provider(User.AuthProvider.GOOGLE)
                .providerId("admin-google-id")
                .role(UserRole.ADMIN)
                .build();

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }
}

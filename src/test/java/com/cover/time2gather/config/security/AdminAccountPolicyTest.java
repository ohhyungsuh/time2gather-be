package com.cover.time2gather.config.security;

import com.cover.time2gather.domain.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAccountPolicyTest {

    @Test
    void configuredKakaoEmailIsAnAdminAccount() {
        AdminAccountPolicy policy = new AdminAccountPolicy(
                "other@example.com, Admin@Example.com "
        );

        assertThat(policy.isAdminAccount(User.AuthProvider.KAKAO, "admin@example.com", true))
                .isTrue();
    }

    @Test
    void sameEmailFromAnotherProviderIsNotAnAdminAccount() {
        AdminAccountPolicy policy = new AdminAccountPolicy("admin@example.com");

        assertThat(policy.isAdminAccount(User.AuthProvider.GOOGLE, "admin@example.com", true))
                .isFalse();
    }

    @Test
    void blankOrUnknownEmailIsNotAnAdminAccount() {
        AdminAccountPolicy policy = new AdminAccountPolicy("");

        assertThat(policy.isAdminAccount(User.AuthProvider.KAKAO, null, true)).isFalse();
        assertThat(policy.isAdminAccount(User.AuthProvider.KAKAO, "admin@example.com", true))
                .isFalse();
    }

    @Test
    void unverifiedKakaoEmailIsNotAnAdminAccount() {
        AdminAccountPolicy policy = new AdminAccountPolicy("admin@example.com");

        assertThat(policy.isAdminAccount(User.AuthProvider.KAKAO, "admin@example.com", false))
                .isFalse();
    }
}

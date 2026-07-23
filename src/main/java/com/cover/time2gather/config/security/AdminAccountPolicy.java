package com.cover.time2gather.config.security;

import com.cover.time2gather.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AdminAccountPolicy {

    private final Set<String> kakaoAdminEmails;

    public AdminAccountPolicy(@Value("${security.admin.kakao-emails:}") String configuredEmails) {
        this.kakaoAdminEmails = Arrays.stream(configuredEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdminAccount(User.AuthProvider provider, String email, boolean emailVerified) {
        return provider == User.AuthProvider.KAKAO
                && emailVerified
                && email != null
                && kakaoAdminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}

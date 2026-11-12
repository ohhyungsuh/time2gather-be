package com.cover.time2gather.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminIdentityServiceTest {
    private final AdminSessionTokens sessions = new AdminSessionTokens("test-admin-key-at-least-256-bits-long", 30);
    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final AdminIdentityService service = new AdminIdentityService(decoder, "client", "owner@gmail.com", "", sessions);

    @Test
    void deniesWhenUnconfiguredWithoutCallingGoogle() {
        var disabled = new AdminIdentityService(decoder, "", "owner@gmail.com", "", sessions);
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.getClientId()).isEmpty();
        assertThatThrownBy(() -> disabled.authenticate("token")).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(decoder);
    }

    @Test
    void rejectsWrongAudienceIssuerExpiredOrUnverifiedIdentity() {
        for (Map<String, Object> override : List.<Map<String, Object>>of(
                Map.of("aud", List.of("other-client")), Map.of("iss", "https://attacker.example"),
                Map.of("exp", Instant.now().minusSeconds(1)), Map.of("email_verified", false),
                Map.of("sub", ""), Map.of("email", "outsider@gmail.com"))) {
            when(decoder.decode("token")).thenReturn(identity(override));
            assertThatThrownBy(() -> service.authenticate("token")).isInstanceOf(BadCredentialsException.class);
        }
    }

    @Test
    void acceptsAuthoritativeGmailAndImmutableSubjectsOnly() {
        when(decoder.decode("token")).thenReturn(identity(Map.of()));
        assertThat(service.authenticate("token").getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        var thirdParty = new AdminIdentityService(decoder, "client", "owner@example.com", "", sessions);
        when(decoder.decode("token")).thenReturn(identity(Map.of("email", "owner@example.com")));
        assertThatThrownBy(() -> thirdParty.authenticate("token")).isInstanceOf(BadCredentialsException.class);
        var subjectAllowed = new AdminIdentityService(decoder, "client", "", "stable-id", sessions);
        assertThat(subjectAllowed.authenticate("token").isAuthenticated()).isTrue();
    }

    @Test
    void exchangesGoogleForThirtyDaySessionAndChecksAllowlistWithoutGoogle() {
        when(decoder.decode("token")).thenReturn(identity(Map.of()));
        var login = service.login("token");
        assertThat(login.credential()).startsWith("t2ga.");
        assertThat(login.identity().expiresAt()).isBetween(Instant.now().plusSeconds(29 * 86400), Instant.now().plusSeconds(30 * 86400));
        clearInvocations(decoder);
        assertThat(service.authenticate(login.credential()).getPrincipal()).isEqualTo(
                new AdminIdentityService.AdminIdentity(login.identity().subject(), login.identity().email(),
                        login.identity().expiresAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)));
        verifyNoInteractions(decoder);
        var removed = new AdminIdentityService(decoder, "client", "different@gmail.com", "", sessions);
        assertThatThrownBy(() -> removed.authenticate(login.credential())).isInstanceOf(BadCredentialsException.class);
        var changedClient = new AdminIdentityService(decoder, "other-client", "owner@gmail.com", "", sessions);
        assertThatThrownBy(() -> changedClient.authenticate(login.credential())).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.login(login.credential())).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void subjectOnlySessionDoesNotBecomeTrustedEmailWhenSubjectIsRemoved() {
        var subjectAllowed = new AdminIdentityService(decoder, "client", "", "stable-id", sessions);
        when(decoder.decode("token")).thenReturn(identity(Map.of("email", "owner@example.com")));
        var login = subjectAllowed.login("token");
        var removed = new AdminIdentityService(decoder, "client", "owner@example.com", "", sessions);
        assertThatThrownBy(() -> removed.authenticate(login.credential())).isInstanceOf(BadCredentialsException.class);
    }

    private Jwt identity(Map<String, Object> overrides) {
        Map<String, Object> claims = new HashMap<>(Map.of(
                "iss", "https://accounts.google.com", "aud", List.of("client"), "sub", "stable-id",
                "iat", Instant.now().minusSeconds(10), "exp", Instant.now().plusSeconds(3600),
                "email", "owner@gmail.com", "email_verified", true));
        claims.putAll(overrides);
        return new Jwt("token", (Instant) claims.get("iat"), (Instant) claims.get("exp"), Map.of("alg", "RS256"), claims);
    }
}

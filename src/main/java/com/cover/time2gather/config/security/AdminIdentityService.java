package com.cover.time2gather.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Admin identity is independent of anonymous users and their role-bearing cookies. */
@Service
public class AdminIdentityService {
    private final JwtDecoder decoder;
    private final String clientId;
    private final Set<String> emails;
    private final Set<String> subjects;
    private final AdminSessionTokens sessions;

    public AdminIdentityService(
            @Qualifier("adminGoogleJwtDecoder") JwtDecoder adminGoogleJwtDecoder,
            @Value("${admin.google.client-id:}") String clientId,
            @Value("${admin.allowed-emails:}") String emails,
            @Value("${admin.allowed-subjects:}") String subjects,
            AdminSessionTokens sessions) {
        this.decoder = adminGoogleJwtDecoder;
        this.clientId = clientId.trim();
        this.emails = parseList(emails.toLowerCase(Locale.ROOT));
        this.subjects = parseList(subjects);
        this.sessions = sessions;
    }

    public String getClientId() { return isEnabled() ? clientId : ""; }

    public boolean isEnabled() {
        return !clientId.isBlank() && (!emails.isEmpty() || !subjects.isEmpty());
    }

    public Authentication authenticate(String token) {
        if (!isEnabled()) throw new BadCredentialsException("Admin login is disabled");
        if (sessions.isSession(token)) {
            var session = sessions.verify(token, clientId);
            requireAllowed(session.subject(), session.email(), session.authoritativeEmail());
            return authenticated(new AdminIdentity(session.subject(), session.email(), session.expiresAt()));
        }
        return authenticateGoogle(token).authentication();
    }

    // Exchange only a freshly verified Google identity; existing sessions cannot extend themselves.
    public SessionLogin login(String googleToken) {
        if (!isEnabled() || sessions.isSession(googleToken)) throw new BadCredentialsException("Invalid admin login");
        var verified = authenticateGoogle(googleToken);
        var identity = (AdminIdentity) verified.authentication().getPrincipal();
        var issued = sessions.issue(identity.subject(), identity.email(), verified.authoritativeEmail(), clientId);
        return new SessionLogin(issued.credential(), new AdminIdentity(identity.subject(), identity.email(), issued.expiresAt()));
    }

    private VerifiedGoogle authenticateGoogle(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            if (jwt == null) throw new BadCredentialsException("Invalid admin identity");
            String issuer = jwt.getClaimAsString("iss");
            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            if (!("https://accounts.google.com".equals(issuer) || "accounts.google.com".equals(issuer))
                    || jwt.getAudience() == null || !jwt.getAudience().contains(clientId)
                    || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())
                    || subject == null || subject.isBlank()
                    || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
                throw new BadCredentialsException("Invalid admin identity");
            }
            // Google is authoritative for Gmail and verified Workspace identities only.
            // Other addresses require the immutable Google subject allowlist.
            String normalizedEmail = email == null ? "" : email.toLowerCase(Locale.ROOT);
            boolean authoritativeEmail = normalizedEmail.endsWith("@gmail.com")
                    || (jwt.getClaimAsString("hd") != null && !jwt.getClaimAsString("hd").isBlank());
            requireAllowed(subject, email, authoritativeEmail);
            return new VerifiedGoogle(authenticated(new AdminIdentity(subject, email, jwt.getExpiresAt())), authoritativeEmail);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid admin identity", ex);
        }
    }

    private void requireAllowed(String subject, String email, boolean authoritativeEmail) {
        String normalizedEmail = email == null ? "" : email.toLowerCase(Locale.ROOT);
        if (!subjects.contains(subject) && !(authoritativeEmail && emails.contains(normalizedEmail))) {
            throw new BadCredentialsException("Admin identity is not allowed");
        }
    }

    private Authentication authenticated(AdminIdentity identity) {
        return UsernamePasswordAuthenticationToken.authenticated(identity, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
    }

    private static Set<String> parseList(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    public record AdminIdentity(String subject, String email, Instant expiresAt) {}
    public record SessionLogin(String credential, AdminIdentity identity) {}
    private record VerifiedGoogle(Authentication authentication, boolean authoritativeEmail) {}
}

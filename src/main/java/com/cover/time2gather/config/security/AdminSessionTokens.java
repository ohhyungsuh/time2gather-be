package com.cover.time2gather.config.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** A purpose-specific key prevents ordinary user JWTs from becoming admin sessions. */
@Component
public class AdminSessionTokens {
    private static final String PREFIX = "t2ga.";
    private static final String ISSUER = "time2gather-admin";
    private static final String AUDIENCE = "time2gather-admin-api";
    private final SecretKey key;
    private final Duration lifetime;

    public AdminSessionTokens(@Value("${jwt.secret}") String secret,
                              @Value("${admin.session.days:30}") int days) {
        if (days < 1 || days > 90) throw new IllegalArgumentException("Admin session days must be between 1 and 90");
        this.lifetime = Duration.ofDays(days);
        try {
            Mac derivation = Mac.getInstance("HmacSHA256");
            derivation.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            this.key = Keys.hmacShaKeyFor(derivation.doFinal("time2gather-admin-session-v1".getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot initialize admin session signing", ex);
        }
    }

    public boolean isSession(String token) { return token.startsWith(PREFIX); }

    public IssuedSession issue(String subject, String email, boolean authoritativeEmail, String clientId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        String token = Jwts.builder().issuer(ISSUER).audience().add(AUDIENCE).and()
                .subject(subject).issuedAt(Date.from(now)).expiration(Date.from(expiresAt))
                .claim("email", email).claim("authoritativeEmail", authoritativeEmail).claim("clientId", clientId)
                .signWith(key, Jwts.SIG.HS256).compact();
        return new IssuedSession(PREFIX + token, expiresAt);
    }

    public VerifiedSession verify(String token, String clientId) {
        try {
            if (!isSession(token)) throw new BadCredentialsException("Invalid admin session");
            var claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).requireAudience(AUDIENCE)
                    .require("clientId", clientId).build().parseSignedClaims(token.substring(PREFIX.length())).getPayload();
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank() || claims.getExpiration() == null || claims.getIssuedAt() == null
                    || !claims.getExpiration().toInstant().isAfter(Instant.now())
                    || claims.getExpiration().toInstant().isAfter(claims.getIssuedAt().toInstant().plus(lifetime))) {
                throw new BadCredentialsException("Invalid admin session");
            }
            return new VerifiedSession(subject, claims.get("email", String.class),
                    Boolean.TRUE.equals(claims.get("authoritativeEmail", Boolean.class)), claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid admin session", ex);
        }
    }

    public record IssuedSession(String credential, Instant expiresAt) {}
    public record VerifiedSession(String subject, String email, boolean authoritativeEmail, Instant expiresAt) {}
}

package com.cover.time2gather.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import java.time.Instant;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import static org.assertj.core.api.Assertions.*;

class AdminSessionTokensTest {
    private static final String SECRET = "test-admin-key-at-least-256-bits-long";
    private final AdminSessionTokens tokens = new AdminSessionTokens(SECRET, 30);

    @Test
    void survivesServiceRestartAndRejectsOtherKeysAndTampering() {
        var issued = tokens.issue("subject", "owner@gmail.com", true, "client");
        assertThat(new AdminSessionTokens(SECRET, 30).verify(issued.credential(), "client").subject()).isEqualTo("subject");
        assertThatThrownBy(() -> new AdminSessionTokens("a-different-test-key-at-least-256-bits", 30)
                .verify(issued.credential(), "client")).isInstanceOf(BadCredentialsException.class);
        String token = issued.credential();
        int signature = token.lastIndexOf('.') + 1;
        String tampered = token.substring(0, signature) + (token.charAt(signature) == 'A' ? 'B' : 'A') + token.substring(signature + 1);
        assertThatThrownBy(() -> tokens.verify(tampered, "client")).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> tokens.verify("t2ga.not-a-jwt", "client")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsExpiredWrongPurposeAndUnboundedSessions() throws Exception {
        Mac derivation = Mac.getInstance("HmacSHA256");
        derivation.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var key = Keys.hmacShaKeyFor(derivation.doFinal("time2gather-admin-session-v1".getBytes(StandardCharsets.UTF_8)));
        for (var scenario : new String[]{"expired", "wrong-issuer", "wrong-audience", "too-long", "missing-expiration"}) {
            Instant now = Instant.now();
            var builder = Jwts.builder().issuer(scenario.equals("wrong-issuer") ? "ordinary-user" : "time2gather-admin")
                    .audience().add(scenario.equals("wrong-audience") ? "ordinary-api" : "time2gather-admin-api").and()
                    .subject("subject").claim("clientId", "client").issuedAt(Date.from(now.minusSeconds(60)));
            if (!scenario.equals("missing-expiration")) builder.expiration(Date.from(
                    scenario.equals("expired") ? now.minusSeconds(1) : now.plusSeconds((scenario.equals("too-long") ? 31L : 1L) * 86400)));
            String token = "t2ga." + builder.signWith(key, Jwts.SIG.HS256).compact();
            assertThatThrownBy(() -> tokens.verify(token, "client")).as(scenario).isInstanceOf(BadCredentialsException.class);
        }
    }
}

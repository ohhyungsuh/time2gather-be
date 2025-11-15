package com.cover.time2gather.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 토큰 생성 및 검증 서비스
 */
@Service
public class JwtTokenService {

    private final String secretKey;
    private final long expirationMs;

    public JwtTokenService(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs
    ) {
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
    }

    /**
	 * JWT 토큰 생성
	 *
	 * @param userId 사용자 ID
	 * @param username 사용자 이름
	 * @return JWT 토큰
	 */
	public String generateToken(Long userId, String username) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("userId", userId);

		Date now = new Date();
		Date expiration = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
			.setClaims(claims)
			.setSubject(username)
			.setIssuedAt(now)
			.setExpiration(expiration)
			.signWith(getSigningKey(), SignatureAlgorithm.HS256)
			.compact();
	}

	/**
	 * JWT 토큰 검증
	 *
	 * @param token JWT 토큰
	 * @return 유효하면 true, 아니면 false
	 */
	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 토큰에서 userId 추출
	 *
	 * @param token JWT 토큰
	 * @return userId
	 */
	public Long extractUserId(String token) {
		Claims claims = extractClaims(token);
		return claims.get("userId", Long.class);
	}

	/**
	 * 토큰에서 username 추출
	 *
	 * @param token JWT 토큰
	 * @return username
	 */
	public String extractUsername(String token) {
		Claims claims = extractClaims(token);
		return claims.getSubject();
	}

	/**
	 * 토큰에서 모든 Claims 추출
	 *
	 * @param token JWT 토큰
	 * @return Claims
	 */
	public Claims extractClaims(String token) {
		return Jwts.parser()
			.setSigningKey(getSigningKey())
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}


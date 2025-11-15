package com.cover.time2gather.domain.auth.service;

import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.auth.oidc.OidcProviderRegistry;
import com.cover.time2gather.domain.auth.oidc.OidcProviderStrategy;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2/OIDC 로그인 처리 서비스
 */
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final OidcProviderRegistry providerRegistry;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public OAuthLoginResult login(String providerName, String authorizationCode) {
        // 1. Provider에서 ID Token 획득
        OidcProviderStrategy provider = providerRegistry.getProvider(providerName);
        String idToken = provider.getIdToken(authorizationCode);

        // 2. ID Token 파싱 (payload 부분만 디코딩)
        Map<String, Object> idTokenClaims = parseIdToken(idToken);
        String providerId = (String) idTokenClaims.get("sub");
        String email = (String) idTokenClaims.get("email");
        String nickname = (String) idTokenClaims.get("nickname");

        // 3. User 조회 or 생성
        User.AuthProvider authProvider = User.AuthProvider.valueOf(providerName.toUpperCase());

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(authProvider, providerId);
        boolean isNewUser = existingUser.isEmpty();

        User user = existingUser.orElseGet(() -> createNewUser(authProvider, providerId, email, nickname));

        // 4. JWT 토큰 생성
        String jwtToken = jwtTokenService.generateToken(user.getId(), user.getUsername());

        return new OAuthLoginResult(
                jwtToken,
                isNewUser,
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }

    private User createNewUser(User.AuthProvider provider, String providerId, String email, String nickname) {
        String username = provider.name().toLowerCase() + "_" + providerId;

        User newUser = User.builder()
                .username(username)
                .email(email)
                .provider(provider)
                .providerId(providerId)
                .build();

        return userRepository.save(newUser);
    }

    private Map<String, Object> parseIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid ID token format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ID token", e);
        }
    }
}

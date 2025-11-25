package com.cover.time2gather.domain.auth.service;

import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.infra.oauth.OidcProviderRegistry;
import com.cover.time2gather.infra.oauth.OidcProviderStrategy;
import com.cover.time2gather.infra.oauth.OidcUserInfo;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public OAuthLoginResult login(String providerName, String authorizationCode, String redirectUri) {
        OidcProviderStrategy provider = providerRegistry.getProvider(providerName);
        OidcUserInfo userInfo = provider.getUserInfo(authorizationCode, redirectUri);

        String providerId = userInfo.getProviderId();
        String email = userInfo.getEmail();
        String profileImageUrl = userInfo.getProfileImageUrl();
		String userName = userInfo.getNickname();

        // 2. User 조회 or 생성
        User.AuthProvider authProvider = User.AuthProvider.valueOf(providerName.toUpperCase());

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(authProvider, providerId);
        boolean isNewUser = existingUser.isEmpty();

        User user;
        if (isNewUser) {
            user = createNewUser(authProvider, providerId, email, profileImageUrl, userName);
        } else {
            user = existingUser.get();
            // 기존 사용자의 profileImageUrl 업데이트
            if (profileImageUrl != null && !profileImageUrl.equals(user.getProfileImageUrl())) {
                user.updateProfileImageUrl(profileImageUrl);
            }
        }

        // 3. JWT 토큰 생성
        String jwtToken = jwtTokenService.generateToken(user.getId(), user.getUsername());

        return new OAuthLoginResult(
                jwtToken,
                isNewUser,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfileImageUrl()
        );
    }

    private User createNewUser(User.AuthProvider provider, String providerId, String email, String profileImageUrl, String nickname) {
        User newUser = User.builder()
                .username(nickname)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .provider(provider)
                .providerId(providerId)
                .build();

        return userRepository.save(newUser);
    }
}

package com.cover.time2gather.config.security;

import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 소셜 로그인(카카오 등) 성공 후 처리 핸들러
 * 사용자를 DB에 저장/조회하고, CustomUserPrincipal로 Authentication을 교체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FederatedIdentityAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            String registrationId = oauth2Token.getAuthorizedClientRegistrationId();

            log.info("OAuth2 login success: provider={}, attributes={}", registrationId, oauth2User.getAttributes());

            // 사용자 정보 추출 및 DB 저장/조회
            User user = findOrCreateUser(registrationId, oauth2User);

            // CustomUserPrincipal로 Authentication 교체 (OAuth2 Authorization Server가 이해할 수 있는 형태)
            CustomUserPrincipal userPrincipal = CustomUserPrincipal.from(user);
            OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                    new CustomOAuth2User(userPrincipal, oauth2User.getAttributes()),
                    userPrincipal.getAuthorities(),
                    registrationId
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            log.info("User authenticated: userId={}, email={}", user.getId(), user.getEmail());
        }

        // 원래 요청했던 URL로 리다이렉트 (OAuth2 authorize 요청)
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            log.info("Redirecting to saved request: {}", targetUrl);
            response.sendRedirect(targetUrl);
        } else {
            response.sendRedirect("/");
        }
    }

    private User findOrCreateUser(String provider, OAuth2User oauth2User) {
        User.AuthProvider authProvider = User.AuthProvider.valueOf(provider.toUpperCase());
        String providerId = extractProviderId(provider, oauth2User);
        
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(authProvider, providerId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // 프로필 이미지 업데이트
            String profileImageUrl = extractProfileImageUrl(provider, oauth2User);
            if (profileImageUrl != null && !profileImageUrl.equals(user.getProfileImageUrl())) {
                user.updateProfileImageUrl(profileImageUrl);
            }
            return user;
        }
        
        // 신규 사용자 생성
        String email = extractEmail(provider, oauth2User);
        String nickname = extractNickname(provider, oauth2User);
        String profileImageUrl = extractProfileImageUrl(provider, oauth2User);
        
        User newUser = User.builder()
                .username(nickname)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .provider(authProvider)
                .providerId(providerId)
                .build();
        
        return userRepository.save(newUser);
    }

    private String extractProviderId(String provider, OAuth2User oauth2User) {
        if ("kakao".equalsIgnoreCase(provider)) {
            Object id = oauth2User.getAttribute("id");
            if (id != null) {
                return String.valueOf(id);
            }
        }
        // 다른 provider 추가 가능
        return oauth2User.getName();
    }

    private String extractEmail(String provider, OAuth2User oauth2User) {
        if ("kakao".equalsIgnoreCase(provider)) {
            Map<String, Object> kakaoAccount = oauth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                return (String) kakaoAccount.get("email");
            }
        }
        return null;
    }

    private String extractNickname(String provider, OAuth2User oauth2User) {
        if ("kakao".equalsIgnoreCase(provider)) {
            Map<String, Object> properties = oauth2User.getAttribute("properties");
            if (properties != null) {
                return (String) properties.get("nickname");
            }
            Map<String, Object> kakaoAccount = oauth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    return (String) profile.get("nickname");
                }
            }
        }
        return "User";
    }

    private String extractProfileImageUrl(String provider, OAuth2User oauth2User) {
        if ("kakao".equalsIgnoreCase(provider)) {
            Map<String, Object> properties = oauth2User.getAttribute("properties");
            if (properties != null) {
                return (String) properties.get("profile_image");
            }
            Map<String, Object> kakaoAccount = oauth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    return (String) profile.get("profile_image_url");
                }
            }
        }
        return null;
    }
}

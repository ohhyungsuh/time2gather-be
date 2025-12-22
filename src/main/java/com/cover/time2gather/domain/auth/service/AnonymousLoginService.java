package com.cover.time2gather.domain.auth.service;

import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Meeting 스코프 기반 익명 로그인 서비스
 */
@Service
@RequiredArgsConstructor
public class AnonymousLoginService {

    private static final String PROVIDER_ID_SEPARATOR = ":";
    private static final String INVALID_PASSWORD_MESSAGE_FORMAT = "Invalid password for user: %s";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    /**
     * Meeting 스코프 기반 providerId 생성
     *
     * @param meetingCode Meeting 코드
     * @param username 사용자 이름
     * @return meeting_code:username 형식의 providerId
     */
    public String generateProviderId(String meetingCode, String username) {
        return meetingCode + PROVIDER_ID_SEPARATOR + username;
    }

    /**
     * 익명 로그인 처리
     * - 신규 유저: 생성 후 JWT 발급
     * - 기존 유저: 비밀번호 검증 후 JWT 발급
     *
     * @param meetingCode Meeting 코드
     * @param username 사용자 이름
     * @param password 비밀번호
     * @return AnonymousLoginResult
     * @throws InvalidPasswordException 비밀번호 불일치 시
     */
    @Transactional
    public AnonymousLoginResult login(String meetingCode, String username, String password) {
        String providerId = generateProviderId(meetingCode, username);

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(
                User.AuthProvider.ANONYMOUS,
                providerId
        );

        User user;
        boolean isNewUser;

        if (existingUser.isEmpty()) {
            // 신규 유저 생성 (username을 displayName으로 전달)
            user = createNewUser(providerId, password, username);
            isNewUser = true;
        } else {
            // 기존 유저 비밀번호 검증
            user = existingUser.get();
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new InvalidPasswordException(
                    String.format(INVALID_PASSWORD_MESSAGE_FORMAT, username)
                );
            }
            isNewUser = false;
        }

        // JWT 생성
        String jwtToken = jwtTokenService.generateToken(user.getId(), user.getUsername());

        return new AnonymousLoginResult(
                jwtToken,
                isNewUser,
                user.getId(),
                username  // Display name (meeting 스코프 제거)
        );
    }

    /**
     * 익명 사용자 생성
     *
     * @param providerId meetingCode:username 형식의 고유 ID
     * @param password 사용자 비밀번호
     * @param displayName 사용자가 입력한 표시 이름
     * @return 생성된 User 엔티티
     */
    private User createNewUser(String providerId, String password, String displayName) {
        String hashedPassword = passwordEncoder.encode(password);
        String profileImageUrl = generateDefaultProfileImage(displayName);

        User newUser = User.builder()
                .username(displayName)  // 사용자 입력 이름만 저장
                .password(hashedPassword)
                .email(null)  // 익명 사용자는 이메일 없음
                .profileImageUrl(profileImageUrl)
                .provider(User.AuthProvider.ANONYMOUS)
                .providerId(providerId)  // meetingCode:displayName 저장
                .build();

        return userRepository.save(newUser);
    }

    /**
     * 기본 프로필 이미지 생성
     * UI Avatars API를 사용하여 사용자 이름 기반 아바타 생성
     *
     * @param username 사용자 이름
     * @return 프로필 이미지 URL
     */
    private String generateDefaultProfileImage(String username) {
        try {
            String encoded = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
            return String.format("https://ui-avatars.com/api/?name=%s&background=random&size=200", encoded);
        } catch (Exception e) {
            return "https://ui-avatars.com/api/?name=User&background=cccccc&size=200";
        }
    }
}


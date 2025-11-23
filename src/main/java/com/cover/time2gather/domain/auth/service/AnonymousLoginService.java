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
            // 신규 유저 생성
            user = createNewUser(providerId, password);
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

    private User createNewUser(String providerId, String password) {
        String hashedPassword = passwordEncoder.encode(password);

        User newUser = User.builder()
                .username(providerId)  // username도 providerId로 설정
                .password(hashedPassword)
                .provider(User.AuthProvider.ANONYMOUS)
                .providerId(providerId)
                .build();

        return userRepository.save(newUser);
    }
}


package com.cover.time2gather.domain.auth.service;

import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 비밀번호 없는 익명 세션 발급 서비스.
 * 표시 이름은 신원 증명이 아니며, 소유권은 추측 불가능한 providerId와 JWT로 구분한다.
 */
@Service
@RequiredArgsConstructor
public class AnonymousLoginService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    /** 추측 불가능한 브라우저 소유권 ID를 생성한다. */
    public String generateProviderId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 익명 로그인 처리
     * 매 호출마다 새 익명 사용자를 생성한다. 같은 표시 이름을 사용해도 서로 다른 사용자이며,
     * 브라우저 쿠키가 유지되는 동안에만 본인의 선택을 수정할 수 있다.
     *
     * @param username 표시 이름
     * @return AnonymousLoginResult
     */
    @Transactional
    public AnonymousLoginResult login(String username) {
        User user = createNewUser(generateProviderId(), username.trim());

        // JWT 생성
        String jwtToken = jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getRole());

        return new AnonymousLoginResult(
                jwtToken,
                true,
                user.getId(),
                user.getUsername()
        );
    }

    /** 기존 익명 사용자의 소유권을 유지하면서 표시 이름과 JWT를 갱신한다. */
    @Transactional
    public AnonymousLoginResult rename(User user, String username) {
        user.updateUsername(username.trim());
        User savedUser = userRepository.save(user);
        String jwtToken = jwtTokenService.generateToken(
                savedUser.getId(), savedUser.getUsername(), savedUser.getRole());

        return new AnonymousLoginResult(
                jwtToken,
                false,
                savedUser.getId(),
                savedUser.getUsername()
        );
    }

    /**
     * 익명 사용자 생성
     *
     * @param providerId 무작위 익명 ID
     * @param displayName 사용자가 입력한 표시 이름
     * @return 생성된 User 엔티티
     */
    private User createNewUser(String providerId, String displayName) {
        User newUser = User.builder()
                .username(displayName)  // 사용자 입력 이름만 저장
                .email(null)  // 익명 사용자는 이메일 없음
                // 닉네임을 외부 아바타 서비스로 전송하지 않고 클라이언트 fallback을 사용한다.
                .profileImageUrl("")
                .provider(User.AuthProvider.ANONYMOUS)
                .providerId(providerId)
                .build();

        return userRepository.save(newUser);
    }

}

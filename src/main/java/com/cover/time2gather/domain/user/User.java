package com.cover.time2gather.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_provider", columnList = "provider, provider_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private static final String USERNAME_DELIMITER = "_";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(length = 255)
    private String email;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public User(String username, String password, String email,
                String profileImageUrl, AuthProvider provider, String providerId) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.provider = provider;
        this.providerId = providerId;
    }

    /**
     * 프로필 이미지 URL 업데이트
     * 도메인 로직: 변경 가능한 속성에 대한 비즈니스 규칙
     */
    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * OAuth Provider와 Provider ID로 username 생성
     * 도메인 로직: username 생성 규칙을 도메인 모델이 담당
     *
     * @param provider OAuth Provider
     * @param providerId Provider에서 제공한 사용자 ID
     * @return 생성된 username
     */
    public static String generateUsername(AuthProvider provider, String providerId) {
        if (provider == null || providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("Provider and providerId must not be null or blank");
        }
        return provider.name().toLowerCase() + USERNAME_DELIMITER + providerId;
    }

    public enum AuthProvider {
        ANONYMOUS,
        KAKAO,
        GOOGLE
    }
}


package com.cover.time2gather.api.auth.dto;

import com.cover.time2gather.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "사용자 정보 응답")
@Getter
@Builder
public class UserInfoResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자명", example = "kakao_1234567890")
    private String username;

    @Schema(description = "이메일 주소", example = "user@example.com", nullable = true)
    private String email;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", nullable = true)
    private String profileImageUrl;

    @Schema(description = "인증 제공자", example = "KAKAO")
    private String provider;

    @Schema(description = "가입 일시", example = "2025-11-15T10:00:00")
    private String createdAt;

    /**
     * User 엔티티로부터 UserInfoResponse 생성
     */
    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .provider(user.getProvider().name())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}


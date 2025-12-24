package com.cover.time2gather.api.auth.dto.response;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

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

    @Schema(description = "익명 사용자가 로그인한 미팅 코드 (익명 유저만 해당)", example = "mtg_abc123xyz", nullable = true)
    private String anonymousMeetingCode;

    @Schema(description = "가입 일시", example = "2025-11-15T10:00:00")
    private String createdAt;

    @Schema(description = "생성한 모임 목록")
    private List<CreatedMeetingInfo> createdMeetings;

    @Schema(description = "참여한 모임 목록")
    private List<ParticipatedMeetingInfo> participatedMeetings;

    /**
     * User 엔티티와 생성한 모임 목록으로부터 UserInfoResponse 생성
     */
    public static UserInfoResponse from(User user, List<Meeting> createdMeetings, List<Meeting> participatedMeetings) {
        // 익명 사용자인 경우 providerId에서 미팅 코드 추출 (형식: meetingCode:username)
        String anonymousMeetingCode = null;
        if (user.getProvider() == User.AuthProvider.ANONYMOUS && user.getProviderId() != null) {
            String[] parts = user.getProviderId().split(":", 2);
            if (parts.length > 0) {
                anonymousMeetingCode = parts[0];
            }
        }

        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .provider(user.getProvider().name())
                .anonymousMeetingCode(anonymousMeetingCode)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .createdMeetings(createdMeetings.stream()
                        .map(CreatedMeetingInfo::from)
                        .collect(Collectors.toList()))
                .participatedMeetings(participatedMeetings.stream()
                        .map(ParticipatedMeetingInfo::from)
                        .collect(Collectors.toList()))
                .build();
    }

    @Schema(description = "생성한 모임 정보")
    @Getter
    @Builder
    public static class CreatedMeetingInfo {
        @Schema(description = "모임 ID", example = "1")
        private Long id;

        @Schema(description = "모임 코드", example = "mtg_a3f8k2md9x")
        private String code;

        @Schema(description = "모임 제목", example = "프로젝트 킥오프 미팅")
        private String title;

        @Schema(description = "모임 설명", example = "2월 신규 프로젝트 시작 회의", nullable = true)
        private String description;

        @Schema(description = "타임존", example = "Asia/Seoul")
        private String timezone;

        @Schema(description = "생성 일시", example = "2025-11-15T10:00:00")
        private String createdAt;

        public static CreatedMeetingInfo from(Meeting meeting) {
            return CreatedMeetingInfo.builder()
                    .id(meeting.getId())
                    .code(meeting.getMeetingCode())
                    .title(meeting.getTitle())
                    .description(meeting.getDescription())
                    .timezone(meeting.getTimezone())
                    .createdAt(meeting.getCreatedAt() != null ? meeting.getCreatedAt().toString() : null)
                    .build();
        }
    }

    @Schema(description = "참여한 모임 정보")
    @Getter
    @Builder
    public static class ParticipatedMeetingInfo {
        @Schema(description = "모임 ID", example = "2")
        private Long id;

        @Schema(description = "모임 코드", example = "mtg_x9k2md3fa8")
        private String code;

        @Schema(description = "모임 제목", example = "팀 정기 회의")
        private String title;

        @Schema(description = "모임 설명", example = "주간 팀 미팅", nullable = true)
        private String description;

        @Schema(description = "타임존", example = "Asia/Seoul")
        private String timezone;

        @Schema(description = "생성 일시", example = "2025-11-15T10:00:00")
        private String createdAt;

        public static ParticipatedMeetingInfo from(Meeting meeting) {
            return ParticipatedMeetingInfo.builder()
                    .id(meeting.getId())
                    .code(meeting.getMeetingCode())
                    .title(meeting.getTitle())
                    .description(meeting.getDescription())
                    .timezone(meeting.getTimezone())
                    .createdAt(meeting.getCreatedAt() != null ? meeting.getCreatedAt().toString() : null)
                    .build();
        }
    }
}


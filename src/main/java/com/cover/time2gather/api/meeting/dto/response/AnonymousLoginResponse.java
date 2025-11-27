package com.cover.time2gather.api.meeting.dto.response;

import com.cover.time2gather.domain.auth.service.AnonymousLoginResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "익명 로그인 응답")
@Getter
@RequiredArgsConstructor
public class AnonymousLoginResponse {

    @Schema(description = "사용자 ID", example = "1")
    private final Long userId;

    @Schema(description = "사용자 표시 이름 (Meeting 스코프 제거)", example = "철수")
    private final String username;

    @Schema(description = "신규 사용자 여부", example = "true")
    @JsonProperty("isNewUser")
    private final boolean isNewUser;

    /**
     * 도메인 결과로부터 응답 DTO 생성
     * DTO 변환 책임을 DTO 자신이 담당 (Single Responsibility Principle)
     *
     * @param loginResult 익명 로그인 도메인 결과
     * @return AnonymousLoginResponse 인스턴스
     */
    public static AnonymousLoginResponse from(AnonymousLoginResult loginResult) {
        return new AnonymousLoginResponse(
                loginResult.getUserId(),
                loginResult.getDisplayName(),
                loginResult.isNewUser()
        );
    }
}

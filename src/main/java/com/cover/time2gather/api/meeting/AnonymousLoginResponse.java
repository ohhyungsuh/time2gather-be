package com.cover.time2gather.api.meeting;

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
}

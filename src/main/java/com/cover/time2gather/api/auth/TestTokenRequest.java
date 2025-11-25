package com.cover.time2gather.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "테스트용 토큰 생성 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TestTokenRequest {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
}


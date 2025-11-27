package com.cover.time2gather.api.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "익명 로그인 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousLoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "사용자 이름 (Meeting 스코프 내에서 유니크)", example = "철수", required = true)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "비밀번호", example = "1234", required = true)
    private String password;
}

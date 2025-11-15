package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.domain.auth.service.AnonymousLoginResult;
import com.cover.time2gather.domain.auth.service.AnonymousLoginService;
import com.cover.time2gather.domain.auth.service.InvalidPasswordException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Meeting 컨텍스트 내 인증 컨트롤러
 * - 익명 로그인 처리
 */
@Tag(name = "모임 인증 API", description = "Meeting 스코프 기반 익명 로그인 API")
@RestController
@RequestMapping("/api/v1/meetings/{meetingCode}/auth")
@RequiredArgsConstructor
public class MeetingAuthController {

    private final AnonymousLoginService anonymousLoginService;

    @Operation(
            summary = "익명 로그인",
            description = "Meeting 스코프 기반 익명 로그인. 같은 모임 내에서만 username이 유니크하며, 다른 모임에서는 같은 이름 사용 가능."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AnonymousLoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 필드 누락 (username 또는 password)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "비밀번호 불일치 (기존 사용자)"
            )
    })
    @PostMapping("/anonymous")
    public ApiResponse<AnonymousLoginResponse> anonymousLogin(
            @Parameter(description = "Meeting 코드", example = "mtg_abc123")
            @PathVariable String meetingCode,
            @RequestBody AnonymousLoginRequest request,
            HttpServletResponse response
    ) {
        // Manual validation
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        AnonymousLoginResult loginResult = anonymousLoginService.login(
                meetingCode,
                request.getUsername(),
                request.getPassword()
        );

        // JWT를 HttpOnly 쿠키에 설정
        Cookie cookie = new Cookie("accessToken", loginResult.getJwtToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour
        response.addCookie(cookie);

        AnonymousLoginResponse responseData = new AnonymousLoginResponse(
                loginResult.getUserId(),
                loginResult.getDisplayName(),
                loginResult.isNewUser()
        );

        return ApiResponse.success(responseData);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleInvalidPassword(InvalidPasswordException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error(e.getMessage());
    }
}


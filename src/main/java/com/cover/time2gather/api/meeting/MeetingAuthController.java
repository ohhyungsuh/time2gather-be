package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.auth.vo.JwtTokenCookie;
import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.api.meeting.dto.request.AnonymousLoginRequest;
import com.cover.time2gather.api.meeting.dto.response.AnonymousLoginResponse;
import com.cover.time2gather.domain.auth.service.AnonymousLoginResult;
import com.cover.time2gather.domain.auth.service.AnonymousLoginService;
import com.cover.time2gather.domain.auth.service.InvalidPasswordException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private static final String ERROR_USERNAME_REQUIRED = "Username is required";
    private static final String ERROR_PASSWORD_REQUIRED = "Password is required";
    private static final String PATH_ANONYMOUS_LOGIN = "/anonymous";
    private static final String PARAM_MEETING_CODE = "meetingCode";
    private static final String EXAMPLE_MEETING_CODE = "mtg_abc123";
    private static final String HTTP_STATUS_200 = "200";
    private static final String HTTP_STATUS_400 = "400";
    private static final String HTTP_STATUS_401 = "401";
    private static final String RESPONSE_DESC_SUCCESS = "로그인 성공";
    private static final String RESPONSE_DESC_MISSING_FIELD = "필수 필드 누락 (username 또는 password)";
    private static final String RESPONSE_DESC_INVALID_PASSWORD = "비밀번호 불일치 (기존 사용자)";

    private final AnonymousLoginService anonymousLoginService;

    @Operation(
            summary = "익명 로그인",
            description = "Meeting 스코프 기반 익명 로그인. 같은 모임 내에서만 username이 유니크하며, 다른 모임에서는 같은 이름 사용 가능."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = HTTP_STATUS_200,
                    description = RESPONSE_DESC_SUCCESS,
                    content = @Content(schema = @Schema(implementation = AnonymousLoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = HTTP_STATUS_400,
                    description = RESPONSE_DESC_MISSING_FIELD
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = HTTP_STATUS_401,
                    description = RESPONSE_DESC_INVALID_PASSWORD
            )
    })
    @PostMapping(PATH_ANONYMOUS_LOGIN)
    public ApiResponse<AnonymousLoginResponse> anonymousLogin(
            @Parameter(description = "Meeting 코드", example = EXAMPLE_MEETING_CODE)
            @PathVariable(PARAM_MEETING_CODE) String meetingCode,
            @RequestBody AnonymousLoginRequest request,
            HttpServletResponse response
    ) {
        // Manual validation
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException(ERROR_USERNAME_REQUIRED);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException(ERROR_PASSWORD_REQUIRED);
        }

        AnonymousLoginResult loginResult = anonymousLoginService.login(
                meetingCode,
                request.getUsername(),
                request.getPassword()
        );

        // JWT를 HttpOnly 쿠키에 설정 (SameSite=None 포함)
        JwtTokenCookie jwtCookie = JwtTokenCookie.from(loginResult.getJwtToken());
        response.setHeader("Set-Cookie", jwtCookie.toSetCookieHeader());

        AnonymousLoginResponse responseData = AnonymousLoginResponse.from(loginResult);

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


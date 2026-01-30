package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.auth.vo.JwtTokenCookie;
import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.api.meeting.dto.request.AnonymousLoginRequest;
import com.cover.time2gather.api.meeting.dto.response.AnonymousLoginResponse;
import com.cover.time2gather.domain.auth.service.AnonymousLoginResult;
import com.cover.time2gather.domain.auth.service.AnonymousLoginService;
import com.cover.time2gather.domain.auth.service.InvalidPasswordException;
import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
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
@Tag(name = "Meeting Auth API", description = "Meeting-scoped anonymous login API")
@RestController
@RequestMapping("/api/v1/meetings/{meetingCode}/auth")
@RequiredArgsConstructor
public class MeetingAuthController {

    private static final String PATH_ANONYMOUS_LOGIN = "/anonymous";
    private static final String PARAM_MEETING_CODE = "meetingCode";
    private static final String EXAMPLE_MEETING_CODE = "mtg_abc123";
    private static final String HTTP_STATUS_200 = "200";
    private static final String HTTP_STATUS_400 = "400";
    private static final String HTTP_STATUS_401 = "401";
    private static final String RESPONSE_DESC_SUCCESS = "Login successful";
    private static final String RESPONSE_DESC_MISSING_FIELD = "Required field missing (username or password)";
    private static final String RESPONSE_DESC_INVALID_PASSWORD = "Password mismatch (existing user)";

    private final AnonymousLoginService anonymousLoginService;

    @Operation(
            summary = "Anonymous login",
            description = "Meeting-scoped anonymous login. Username is unique only within the same meeting; the same name can be used in different meetings."
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
            throw new BusinessException(ErrorCode.VALIDATION_USERNAME_REQUIRED);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_PASSWORD_REQUIRED);
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
}


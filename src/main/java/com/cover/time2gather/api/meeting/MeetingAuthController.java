package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.auth.vo.JwtTokenCookie;
import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.api.meeting.dto.request.AnonymousLoginRequest;
import com.cover.time2gather.api.meeting.dto.response.AnonymousLoginResponse;
import com.cover.time2gather.domain.auth.service.AnonymousLoginResult;
import com.cover.time2gather.domain.auth.service.AnonymousLoginService;
import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 기존 클라이언트 호환용 모임 경로의 익명 세션 컨트롤러.
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
    private static final String RESPONSE_DESC_SUCCESS = "Login successful";
    private static final String RESPONSE_DESC_MISSING_FIELD = "Required field missing (username)";

    private final AnonymousLoginService anonymousLoginService;

    @Operation(
            summary = "Anonymous login",
            description = "Creates a passwordless anonymous browser session. Display names are not unique."
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
            )
    })
    @PostMapping(PATH_ANONYMOUS_LOGIN)
    public ApiResponse<AnonymousLoginResponse> anonymousLogin(
            @Parameter(description = "Meeting 코드", example = EXAMPLE_MEETING_CODE)
            @PathVariable(PARAM_MEETING_CODE) String meetingCode,
            @Valid @RequestBody AnonymousLoginRequest request,
            HttpServletResponse response
    ) {
        // Manual validation
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_USERNAME_REQUIRED);
        }
        AnonymousLoginResult loginResult = anonymousLoginService.login(request.getUsername());

        // JWT를 HttpOnly 쿠키에 설정 (SameSite=None 포함)
        JwtTokenCookie jwtCookie = JwtTokenCookie.from(loginResult.getJwtToken());
        response.setHeader("Set-Cookie", jwtCookie.toSetCookieHeader());

        AnonymousLoginResponse responseData = AnonymousLoginResponse.from(loginResult);

        return ApiResponse.success(responseData);
    }
}

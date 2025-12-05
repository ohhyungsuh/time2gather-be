package com.cover.time2gather.api.auth;

import com.cover.time2gather.api.auth.dto.request.OAuthLoginRequest;
import com.cover.time2gather.api.auth.dto.response.OAuthLoginResponse;
import com.cover.time2gather.api.auth.dto.response.UserInfoResponse;
import com.cover.time2gather.api.auth.vo.JwtTokenCookie;
import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.config.security.CurrentUser;
import com.cover.time2gather.domain.auth.service.OAuthLoginResult;
import com.cover.time2gather.domain.auth.service.OAuthLoginService;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "인증 API", description = "OAuth2/OIDC 로그인 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ERROR_MESSAGE_OAUTH_LOGIN_FAILED = "OAuth login failed: %s";
    private static final String PATH_OAUTH_LOGIN = "/oauth/{provider}";
    private static final String PARAM_PROVIDER = "provider";
    private static final String EXAMPLE_PROVIDER = "kakao";
    private static final String HTTP_STATUS_200 = "200";
    private static final String HTTP_STATUS_400 = "400";
    private static final String HTTP_STATUS_500 = "500";
    private static final String RESPONSE_DESC_SUCCESS = "로그인 성공";
    private static final String RESPONSE_DESC_INVALID_CODE = "유효하지 않거나 만료된 Authorization Code";
    private static final String RESPONSE_DESC_PROVIDER_ERROR = "OAuth Provider 통신 실패";

    private final OAuthLoginService oAuthLoginService;
    private final MeetingRepository meetingRepository;
    private final com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository meetingUserSelectionRepository;

    @Value("${oauth.redirect.default}")
    private String defaultRedirectUrl;

    @Operation(
            summary = "OAuth 로그인",
            description = "카카오/구글 등의 OAuth Provider를 통한 로그인. Authorization Code를 받아 JWT 토큰을 발급합니다. Redirect URL을 요청에 포함할 수 있으며, 없으면 기본값을 사용합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = HTTP_STATUS_200,
                    description = RESPONSE_DESC_SUCCESS,
                    content = @Content(schema = @Schema(implementation = OAuthLoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = HTTP_STATUS_400,
                    description = RESPONSE_DESC_INVALID_CODE
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = HTTP_STATUS_500,
                    description = RESPONSE_DESC_PROVIDER_ERROR
            )
    })
    @PostMapping(PATH_OAUTH_LOGIN)
    public ApiResponse<OAuthLoginResponse> oauthLogin(
            @Parameter(description = "OAuth Provider (kakao, google 등)", example = EXAMPLE_PROVIDER)
            @PathVariable(PARAM_PROVIDER) String provider,
            @RequestBody OAuthLoginRequest request,
            HttpServletResponse response
    ) {
        // Redirect URL: 요청에 포함되어 있으면 사용, 없으면 기본값 사용
        String redirectUrl = (request.getRedirectUrl() != null && !request.getRedirectUrl().isBlank())
                ? request.getRedirectUrl()
                : defaultRedirectUrl;

        OAuthLoginResult loginResult = oAuthLoginService.login(
                provider,
                request.getAuthorizationCode(),
                redirectUrl
        );

        // JWT를 HttpOnly 쿠키에 설정 (Value Object 패턴)
        JwtTokenCookie jwtCookie = JwtTokenCookie.from(loginResult.getJwtToken());
        // SameSite=None을 포함한 Set-Cookie 헤더 직접 설정
        response.setHeader("Set-Cookie", jwtCookie.toSetCookieHeader());

        // DTO 변환 (DTO의 책임)
        OAuthLoginResponse responseData = OAuthLoginResponse.from(loginResult, provider);

        return ApiResponse.success(responseData);
    }

    @Operation(
            summary = "현재 사용자 정보 조회",
            description = "로그인된 사용자의 기본 정보와 생성한 모임 목록, 참여한 모임 목록을 조회합니다. JWT 토큰이 필요합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = HTTP_STATUS_200,
                    description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser(@CurrentUser User user) {
        // 생성한 모임 목록
        List<Meeting> createdMeetings = meetingRepository.findByHostUserId(user.getId());

        // 참여한 모임 목록 (시간 선택을 한 모임들)
        List<Meeting> participatedMeetings = meetingUserSelectionRepository.findAllByUserId(user.getId())
                .stream()
                .map(selection -> meetingRepository.findById(selection.getMeetingId()).orElse(null))
                .filter(meeting -> meeting != null && !meeting.getHostUserId().equals(user.getId())) // 자신이 만든 모임 제외
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        UserInfoResponse response = UserInfoResponse.from(user, createdMeetings, participatedMeetings);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "[테스트용] JWT 토큰 생성",
            description = "개발/테스트 환경에서 사용할 JWT 토큰을 직접 생성합니다. User ID를 입력하면 해당 사용자의 JWT 토큰을 발급받아 Swagger UI에서 테스트할 수 있습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = HTTP_STATUS_200,
                    description = "토큰 생성 성공",
                    content = @Content(schema = @Schema(implementation = TestTokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = HTTP_STATUS_400,
                    description = "존재하지 않는 사용자 ID"
            )
    })
    @PostMapping("/test-token")
    public ApiResponse<TestTokenResponse> generateTestToken(
            @RequestBody TestTokenRequest request,
            HttpServletResponse response
    ) {
        OAuthLoginResult loginResult = oAuthLoginService.generateTestToken(request.getUserId());

        // JWT를 HttpOnly 쿠키에 설정 (SameSite=None 포함)
        JwtTokenCookie jwtCookie = JwtTokenCookie.from(loginResult.getJwtToken());
        response.setHeader("Set-Cookie", jwtCookie.toSetCookieHeader());

        TestTokenResponse responseData = TestTokenResponse.from(loginResult);
        return ApiResponse.success(responseData);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleIllegalState(IllegalStateException e) {
        return ApiResponse.error(String.format(ERROR_MESSAGE_OAUTH_LOGIN_FAILED, e.getMessage()));
    }
}


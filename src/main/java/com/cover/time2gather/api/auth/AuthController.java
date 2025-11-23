package com.cover.time2gather.api.auth;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.domain.auth.service.OAuthLoginResult;
import com.cover.time2gather.domain.auth.service.OAuthLoginService;
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

    @Operation(
            summary = "OAuth 로그인",
            description = "카카오/구글 등의 OAuth Provider를 통한 로그인. Authorization Code를 받아 JWT 토큰을 발급합니다."
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
        OAuthLoginResult loginResult = oAuthLoginService.login(provider, request.getAuthorizationCode());

        // JWT를 HttpOnly 쿠키에 설정 (Value Object 패턴)
        JwtTokenCookie jwtCookie = JwtTokenCookie.from(loginResult.getJwtToken());
        response.addCookie(jwtCookie.getCookie());

        // DTO 변환 (DTO의 책임)
        OAuthLoginResponse responseData = OAuthLoginResponse.from(loginResult, provider);

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


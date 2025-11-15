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

    private final OAuthLoginService oAuthLoginService;

    @Operation(
            summary = "OAuth 로그인",
            description = "카카오/구글 등의 OAuth Provider를 통한 로그인. Authorization Code를 받아 JWT 토큰을 발급합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = OAuthLoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않거나 만료된 Authorization Code"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "OAuth Provider 통신 실패"
            )
    })
    @PostMapping("/oauth/{provider}")
    public ApiResponse<OAuthLoginResponse> oauthLogin(
            @Parameter(description = "OAuth Provider (kakao, google 등)", example = "kakao")
            @PathVariable String provider,
            @RequestBody OAuthLoginRequest request,
            HttpServletResponse response
    ) {
        OAuthLoginResult loginResult = oAuthLoginService.login(provider, request.getAuthorizationCode());

        // JWT를 HttpOnly 쿠키에 설정
        Cookie cookie = new Cookie("accessToken", loginResult.getJwtToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour
        response.addCookie(cookie);

        OAuthLoginResponse responseData = new OAuthLoginResponse(
                loginResult.getUserId(),
                loginResult.getUsername(),
                loginResult.getEmail(),
                provider,
                loginResult.isNewUser()
        );

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
        return ApiResponse.error("OAuth login failed: " + e.getMessage());
    }
}


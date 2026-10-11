package com.cover.time2gather.api.auth;

import com.cover.time2gather.api.auth.dto.response.UserInfoResponse;
import com.cover.time2gather.api.auth.vo.JwtTokenCookie;
import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.api.meeting.dto.request.AnonymousLoginRequest;
import com.cover.time2gather.api.meeting.dto.response.AnonymousLoginResponse;
import com.cover.time2gather.config.security.CurrentUser;
import com.cover.time2gather.domain.auth.service.AnonymousLoginResult;
import com.cover.time2gather.domain.auth.service.AnonymousLoginService;
import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "익명 세션 API", description = "비밀번호 없는 브라우저 기반 익명 세션")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AnonymousLoginService anonymousLoginService;
    private final UserService userService;

    @Operation(summary = "익명 세션 시작", description = "표시 이름만 받아 새 익명 브라우저 세션을 발급합니다.")
    @PostMapping("/anonymous")
    public ApiResponse<AnonymousLoginResponse> anonymousLogin(
            @Valid @RequestBody AnonymousLoginRequest request,
            HttpServletResponse response
    ) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_USERNAME_REQUIRED);
        }
        AnonymousLoginResult loginResult = anonymousLoginService.login(request.getUsername());
        response.setHeader("Set-Cookie", JwtTokenCookie.from(loginResult.getJwtToken()).toSetCookieHeader());
        return ApiResponse.success(AnonymousLoginResponse.from(loginResult));
    }

    @Operation(summary = "익명 사용자 닉네임 변경", description = "현재 익명 사용자의 소유권을 유지하면서 표시 이름을 변경합니다.")
    @PutMapping("/me")
    public ApiResponse<AnonymousLoginResponse> updateUsername(
            @CurrentUser User user,
            @Valid @RequestBody AnonymousLoginRequest request,
            HttpServletResponse response
    ) {
        AnonymousLoginResult loginResult = anonymousLoginService.rename(user, request.getUsername());
        response.setHeader("Set-Cookie", JwtTokenCookie.from(loginResult.getJwtToken()).toSetCookieHeader());
        return ApiResponse.success(AnonymousLoginResponse.from(loginResult));
    }

    @Operation(summary = "현재 익명 사용자 정보 조회", security = @SecurityRequirement(name = "cookieAuth"))
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser(@CurrentUser User user) {
        List<Meeting> createdMeetings = userService.getCreatedMeetings(user.getId());
        List<Meeting> participatedMeetings = userService.getParticipatedMeetings(user.getId());
        return ApiResponse.success(UserInfoResponse.from(user, createdMeetings, participatedMeetings));
    }
}

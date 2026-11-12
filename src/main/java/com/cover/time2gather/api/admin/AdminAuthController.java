package com.cover.time2gather.api.admin;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.config.security.AdminIdentityService;
import com.cover.time2gather.config.security.AdminIdentityService.AdminIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.util.MessageProvider;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminIdentityService identities;

    @GetMapping("/config")
    public ApiResponse<LoginConfig> config() {
        return ApiResponse.success(new LoginConfig(identities.isEnabled(), identities.getClientId()));
    }

    @GetMapping("/session")
    public ApiResponse<AdminIdentity> session(@AuthenticationPrincipal AdminIdentity identity) {
        return ApiResponse.success(identity);
    }

    @PostMapping("/session")
    public ApiResponse<AdminIdentityService.SessionLogin> login(@RequestHeader("Authorization") String authorization) {
        if (!authorization.startsWith("Bearer ")) throw new BadCredentialsException("Invalid admin login");
        return ApiResponse.success(identities.login(authorization.substring(7)));
    }

    public record LoginConfig(boolean enabled, String clientId) {}

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> rejectedLogin() {
        return ResponseEntity.status(401).body(ApiResponse.error(
                MessageProvider.getMessage(ErrorCode.AUTH_REQUIRED_LOGIN.getMessageKey())));
    }
}

package com.cover.time2gather.api.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AWS ALB 등의 헬스체크를 위한 컨트롤러
 */
@Tag(name = "헬스체크 API", description = "서버 상태 확인")
@RestController
public class HealthCheckController {

    @Value("${oauth2.server.playmcp.client-id:NOT_SET}")
    private String playMcpClientId;

    @Value("${oauth2.server.playmcp.client-secret:NOT_SET}")
    private String playMcpClientSecret;

    @Value("${oauth2.server.playmcp.redirect-uri:NOT_SET}")
    private String playMcpRedirectUri;

    @Operation(summary = "헬스체크", description = "서버가 정상 작동 중인지 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponse> healthCheck() {
        return ResponseEntity.ok(new HealthCheckResponse("UP", LocalDateTime.now().toString()));
    }

    /**
     * 임시 디버깅 엔드포인트 - OAuth2 클라이언트 설정 확인
     * TODO: 디버깅 완료 후 삭제
     */
    @Operation(summary = "OAuth2 디버그", description = "OAuth2 클라이언트 설정 확인 (임시)")
    @GetMapping("/debug/oauth2")
    public ResponseEntity<Map<String, String>> debugOAuth2() {
        Map<String, String> info = new HashMap<>();
        info.put("client_id", playMcpClientId);
        info.put("client_secret_prefix", playMcpClientSecret.length() > 5 
            ? playMcpClientSecret.substring(0, 5) + "..." 
            : "TOO_SHORT");
        info.put("client_secret_length", String.valueOf(playMcpClientSecret.length()));
        info.put("redirect_uri", playMcpRedirectUri);
        return ResponseEntity.ok(info);
    }

    @Getter
    public static class HealthCheckResponse {
        private final String status;
        private final String timestamp;

        public HealthCheckResponse(String status, String timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }
    }
}


package com.cover.time2gather.api.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth 2.0 Protected Resource Metadata (RFC 9728) & Authorization Server Metadata
 * MCP 클라이언트가 Authorization Server를 찾을 수 있도록 메타데이터를 제공
 * 
 * PlayMCP 연동을 위해 다음 엔드포인트 필요:
 * - /.well-known/oauth-protected-resource (RFC 9728)
 * - /.well-known/oauth-authorization-server/mcp (MCP 스펙)
 * 
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9728">RFC 9728</a>
 * @see <a href="https://modelcontextprotocol.io/specification/2025-03-26/basic/authorization">MCP Authorization</a>
 */
@RestController
public class OAuth2ProtectedResourceMetadataController {

    @Value("${oauth2.server.issuer:https://api.time2gather.org}")
    private String issuer;

    /**
     * Protected Resource Metadata 엔드포인트
     * MCP 스펙에 따라 authorization_servers 필드를 포함해야 함
     */
    @GetMapping(value = "/.well-known/oauth-protected-resource", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getProtectedResourceMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        // Resource identifier (this MCP server)
        metadata.put("resource", issuer);
        
        // Authorization servers that can issue tokens for this resource
        metadata.put("authorization_servers", List.of(issuer));
        
        // Supported scopes for this resource
        metadata.put("scopes_supported", Arrays.asList(
                "openid",
                "profile",
                "meeting:read",
                "meeting:write"
        ));
        
        // Bearer token methods supported
        metadata.put("bearer_methods_supported", Arrays.asList("header"));
        
        // Resource documentation
        metadata.put("resource_documentation", "https://time2gather.org/docs/api");
        
        return metadata;
    }

    /**
     * MCP 엔드포인트용 Protected Resource Metadata
     * /mcp 경로에 대한 메타데이터
     */
    @GetMapping(value = "/.well-known/oauth-protected-resource/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getMcpProtectedResourceMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("resource", issuer + "/mcp");
        metadata.put("authorization_servers", List.of(issuer));
        metadata.put("scopes_supported", Arrays.asList(
                "openid",
                "profile", 
                "meeting:read",
                "meeting:write"
        ));
        metadata.put("bearer_methods_supported", Arrays.asList("header"));
        
        return metadata;
    }

    /**
     * OAuth Authorization Server Metadata for MCP endpoint
     * MCP 스펙에 따라 클라이언트가 /.well-known/oauth-authorization-server/{path}로 요청
     * 
     * PlayMCP가 /mcp 엔드포인트 접근 시:
     * 1. 401 Unauthorized 수신
     * 2. /.well-known/oauth-authorization-server/mcp 로 AS 정보 요청
     * 3. 이 응답의 authorization_endpoint, token_endpoint 사용
     */
    @GetMapping(value = "/.well-known/oauth-authorization-server/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getOAuthAuthorizationServerMetadataForMcp() {
        Map<String, Object> metadata = new HashMap<>();
        
        // Authorization Server 정보
        metadata.put("issuer", issuer);
        metadata.put("authorization_endpoint", issuer + "/oauth2/authorize");
        metadata.put("token_endpoint", issuer + "/oauth2/token");
        metadata.put("jwks_uri", issuer + "/oauth2/jwks");
        metadata.put("userinfo_endpoint", issuer + "/userinfo");
        
        // 지원 기능 명시
        metadata.put("response_types_supported", Arrays.asList("code"));
        metadata.put("grant_types_supported", Arrays.asList("authorization_code", "refresh_token"));
        metadata.put("subject_types_supported", Arrays.asList("public"));
        metadata.put("id_token_signing_alg_values_supported", Arrays.asList("RS256"));
        metadata.put("scopes_supported", Arrays.asList("openid", "profile", "email", "meeting:read", "meeting:write"));
        metadata.put("token_endpoint_auth_methods_supported", Arrays.asList("client_secret_basic", "client_secret_post"));
        metadata.put("code_challenge_methods_supported", Arrays.asList("S256", "plain"));
        
        return metadata;
    }

    /**
     * OAuth Authorization Server Metadata (기본 경로)
     * 일부 클라이언트는 경로 없이 요청할 수 있음
     */
    @GetMapping(value = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getOAuthAuthorizationServerMetadata() {
        return getOAuthAuthorizationServerMetadataForMcp();
    }

    /**
     * MCP SSE 엔드포인트용 Protected Resource Metadata (레거시 호환)
     */
    @GetMapping(value = "/.well-known/oauth-protected-resource/sse", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getSseProtectedResourceMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("resource", issuer + "/sse");
        metadata.put("authorization_servers", List.of(issuer));
        metadata.put("scopes_supported", Arrays.asList(
                "openid",
                "profile", 
                "meeting:read",
                "meeting:write"
        ));
        metadata.put("bearer_methods_supported", Arrays.asList("header"));
        
        return metadata;
    }

    /**
     * OAuth Authorization Server Metadata for SSE endpoint (레거시 호환)
     */
    @GetMapping(value = "/.well-known/oauth-authorization-server/sse", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getOAuthAuthorizationServerMetadataForSse() {
        return getOAuthAuthorizationServerMetadataForMcp();
    }
}

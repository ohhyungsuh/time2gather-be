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
 * OAuth 2.0 Protected Resource Metadata (RFC 9728)
 * MCP 클라이언트가 Authorization Server를 찾을 수 있도록 메타데이터를 제공
 * 
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9728">RFC 9728</a>
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
     * MCP SSE 엔드포인트용 Protected Resource Metadata
     * /sse 경로에 대한 별도 메타데이터 (선택적)
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
}

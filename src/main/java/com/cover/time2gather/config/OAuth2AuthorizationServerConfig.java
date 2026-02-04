package com.cover.time2gather.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * OAuth2 Authorization Server 설정
 * PlayMCP 연동을 위한 OAuth2 Provider 역할
 */
@Configuration
public class OAuth2AuthorizationServerConfig {

    @Value("${oauth2.server.playmcp.client-id:playmcp-client}")
    private String playMcpClientId;

    @Value("${oauth2.server.playmcp.client-secret:playmcp-secret}")
    private String playMcpClientSecret;

    @Value("${oauth2.server.playmcp.redirect-uri:https://playmcp.com/oauth/callback}")
    private String playMcpRedirectUri;

    @Value("${oauth2.server.issuer:https://api.time2gather.org}")
    private String issuer;

    /**
     * Authorization Server Security Filter Chain
     * OAuth2 엔드포인트 보호 (/oauth2/authorize, /oauth2/token 등)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(oidc -> oidc
                        .providerConfigurationEndpoint(providerConfig -> providerConfig
                                .providerConfigurationCustomizer(customizer -> {
                                    // scopes_supported에 지원하는 모든 scope 추가
                                    customizer.scopes(scopes -> {
                                        scopes.add(OidcScopes.OPENID);
                                        scopes.add(OidcScopes.PROFILE);
                                        scopes.add(OidcScopes.EMAIL);
                                        scopes.add("meeting:read");
                                        scopes.add("meeting:write");
                                    });
                                })
                        )
                );
        
        http
                .cors(cors -> cors.configurationSource(authServerCorsConfigurationSource()))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                );
        
        return http.build();
    }

    /**
     * Authorization Server용 CORS 설정
     */
    private CorsConfigurationSource authServerCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://playmcp.kakao.com",
                "https://*.playmcp.kakao.com",
                "https://time2gather.org",
                "https://*.time2gather.org",
                "http://localhost:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * PlayMCP 클라이언트 등록
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient playMcpClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(playMcpClientId)
                .clientSecret("{noop}" + playMcpClientSecret) // {noop} = plain text password encoder
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(playMcpRedirectUri)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .scope("meeting:read")
                .scope("meeting:write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true) // 사용자 동의 화면 표시
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(playMcpClient);
    }

    /**
     * Authorization Server 설정
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .jwkSetEndpoint("/oauth2/jwks")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .oidcUserInfoEndpoint("/userinfo")
                .build();
    }

    /**
     * JWT 서명을 위한 JWK Source (RSA 키쌍)
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * JWT Decoder (Resource Server에서 토큰 검증용)
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * RSA 키쌍 생성
     * TODO: 프로덕션에서는 외부 저장소(KeyStore, Vault 등)에서 로드해야 함
     */
    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }
}

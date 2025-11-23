package com.cover.time2gather.infra.oauth;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OIDC Provider 전략들을 관리하는 레지스트리
 */
@Component
public class OidcProviderRegistry {

    private final Map<String, OidcProviderStrategy> providers;

    public OidcProviderRegistry(List<OidcProviderStrategy> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        OidcProviderStrategy::getProviderName,
                        Function.identity()
                ));
    }

    /**
     * Provider 이름으로 전략 조회
     *
     * @param providerName provider 이름 (kakao, google 등)
     * @return OidcProviderStrategy 구현체
     * @throws IllegalArgumentException provider를 찾을 수 없는 경우
     */
    public OidcProviderStrategy getProvider(String providerName) {
        OidcProviderStrategy provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown OIDC provider: " + providerName);
        }
        return provider;
    }
}


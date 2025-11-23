package com.cover.time2gather.infra.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OidcProviderStrategyTest {

    @Test
    void shouldDefineOidcProviderStrategyInterface() {
        // Given: OidcProviderStrategy 인터페이스가 정의되어야 함

        // Then: 인터페이스가 존재하고 필요한 메서드가 정의되어야 함
        assertThat(OidcProviderStrategy.class.isInterface()).isTrue();
    }

    @Test
    void shouldHaveGetIdTokenMethod() throws NoSuchMethodException {
        // Given: OidcProviderStrategy 인터페이스

        // Then: getIdToken(authorizationCode) 메서드가 존재해야 함
        var method = OidcProviderStrategy.class.getMethod("getIdToken", String.class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(String.class);
    }

    @Test
    void shouldHaveGetProviderNameMethod() throws NoSuchMethodException {
        // Given: OidcProviderStrategy 인터페이스

        // Then: getProviderName() 메서드가 존재해야 함
        var method = OidcProviderStrategy.class.getMethod("getProviderName");
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(String.class);
    }
}


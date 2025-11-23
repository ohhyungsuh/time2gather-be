package com.cover.time2gather.infra.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcProviderRegistryTest {

    @Mock
    private OidcProviderStrategy kakaoProvider;

    @Mock
    private OidcProviderStrategy googleProvider;

    private OidcProviderRegistry registry;

    @BeforeEach
    void setUp() {
        when(kakaoProvider.getProviderName()).thenReturn("kakao");
        when(googleProvider.getProviderName()).thenReturn("google");

        registry = new OidcProviderRegistry(List.of(kakaoProvider, googleProvider));
    }

    @Test
    void shouldGetProviderByName() {
        // When
        OidcProviderStrategy provider = registry.getProvider("kakao");

        // Then
        assertThat(provider).isEqualTo(kakaoProvider);
    }

    @Test
    void shouldGetGoogleProvider() {
        // When
        OidcProviderStrategy provider = registry.getProvider("google");

        // Then
        assertThat(provider).isEqualTo(googleProvider);
    }

    @Test
    void shouldThrowExceptionWhenProviderNotFound() {
        // When & Then
        assertThatThrownBy(() -> registry.getProvider("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown OIDC provider: unknown");
    }
}


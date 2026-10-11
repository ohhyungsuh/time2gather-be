package com.cover.time2gather.domain.auth.service;

import com.cover.time2gather.domain.auth.jwt.JwtTokenService;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnonymousLoginServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private JwtTokenService jwtTokenService;
    private AnonymousLoginService anonymousLoginService;

    @BeforeEach
    void setUp() {
        anonymousLoginService = new AnonymousLoginService(userRepository, jwtTokenService);
    }

    @Test
    void shouldCreatePasswordlessUserWithUnpredictableIdentity() {
        User savedUser = User.builder().username("철수")
                .provider(User.AuthProvider.ANONYMOUS).providerId("generated-id").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenService.generateToken(any(), any(), any())).thenReturn("jwt-token");

        AnonymousLoginResult result = anonymousLoginService.login("  철수  ");

        assertThat(result.getJwtToken()).isEqualTo("jwt-token");
        assertThat(result.isNewUser()).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("철수");
        assertThat(captor.getValue().getProfileImageUrl()).isEmpty();
        assertThat(captor.getValue().getProvider()).isEqualTo(User.AuthProvider.ANONYMOUS);
        assertThat(captor.getValue().getProviderId()).isNotBlank();
    }

    @Test
    void shouldGenerateDifferentIdentityEveryTime() {
        assertThat(anonymousLoginService.generateProviderId())
                .isNotEqualTo(anonymousLoginService.generateProviderId());
    }
}

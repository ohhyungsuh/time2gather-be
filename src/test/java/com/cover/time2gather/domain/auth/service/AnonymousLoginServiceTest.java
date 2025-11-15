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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnonymousLoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private AnonymousLoginService anonymousLoginService;

    @BeforeEach
    void setUp() {
        anonymousLoginService = new AnonymousLoginService(userRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void shouldGenerateMeetingScopedProviderId() {
        // Given
        String meetingCode = "mtg_abc123";
        String username = "철수";

        // When
        String providerId = anonymousLoginService.generateProviderId(meetingCode, username);

        // Then
        assertThat(providerId).isEqualTo("mtg_abc123:철수");
    }

    @Test
    void shouldCreateNewAnonymousUser() {
        // Given
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String password = "1234";
        String providerId = "mtg_abc123:철수";

        when(passwordEncoder.encode(password)).thenReturn("$2a$10$hashed");
        when(userRepository.findByProviderAndProviderId(User.AuthProvider.ANONYMOUS, providerId))
                .thenReturn(Optional.empty());

        User savedUser = User.builder()
                .username(providerId)
                .password("$2a$10$hashed")
                .provider(User.AuthProvider.ANONYMOUS)
                .providerId(providerId)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenService.generateToken(any(), any())).thenReturn("jwt-token");

        // When
        AnonymousLoginResult result = anonymousLoginService.login(meetingCode, username, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJwtToken()).isEqualTo("jwt-token");
        assertThat(result.isNewUser()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getProvider()).isEqualTo(User.AuthProvider.ANONYMOUS);
        assertThat(capturedUser.getProviderId()).isEqualTo(providerId);
        assertThat(capturedUser.getUsername()).isEqualTo(providerId);
        assertThat(capturedUser.getPassword()).isEqualTo("$2a$10$hashed");

        verify(passwordEncoder).encode(password);
    }

    @Test
    void shouldLoginExistingUserWithCorrectPassword() {
        // Given
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String password = "1234";
        String providerId = "mtg_abc123:철수";

        User existingUser = User.builder()
                .username(providerId)
                .password("$2a$10$hashed")
                .provider(User.AuthProvider.ANONYMOUS)
                .providerId(providerId)
                .build();

        when(userRepository.findByProviderAndProviderId(User.AuthProvider.ANONYMOUS, providerId))
                .thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(password, existingUser.getPassword())).thenReturn(true);
        when(jwtTokenService.generateToken(any(), any())).thenReturn("jwt-token");

        // When
        AnonymousLoginResult result = anonymousLoginService.login(meetingCode, username, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJwtToken()).isEqualTo("jwt-token");
        assertThat(result.isNewUser()).isFalse();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder).matches(password, existingUser.getPassword());
    }

    @Test
    void shouldThrowExceptionWhenPasswordIncorrect() {
        // Given
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String password = "wrong-password";
        String providerId = "mtg_abc123:철수";

        User existingUser = User.builder()
                .username(providerId)
                .password("$2a$10$hashed")
                .provider(User.AuthProvider.ANONYMOUS)
                .providerId(providerId)
                .build();

        when(userRepository.findByProviderAndProviderId(User.AuthProvider.ANONYMOUS, providerId))
                .thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(password, existingUser.getPassword())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> anonymousLoginService.login(meetingCode, username, password))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Invalid password");

        verify(userRepository, never()).save(any());
        verify(jwtTokenService, never()).generateToken(any(), any());
    }

    @Test
    void shouldAllowSameUsernameInDifferentMeetings() {
        // Given
        String username = "철수";

        // When
        String providerId1 = anonymousLoginService.generateProviderId("mtg_abc", username);
        String providerId2 = anonymousLoginService.generateProviderId("mtg_xyz", username);

        // Then
        assertThat(providerId1).isEqualTo("mtg_abc:철수");
        assertThat(providerId2).isEqualTo("mtg_xyz:철수");
        assertThat(providerId1).isNotEqualTo(providerId2);
    }
}


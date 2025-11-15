package com.cover.time2gather.integration;

import com.cover.time2gather.api.meeting.AnonymousLoginRequest;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 익명 로그인 통합 테스트
 * Meeting 스코프 기반 익명 사용자 생성 및 인증 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AnonymousLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateNewAnonymousUserInMeetingScope() throws Exception {
        // Given
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String password = "1234";

        AnonymousLoginRequest request = new AnonymousLoginRequest(username, password);

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("철수"))
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().httpOnly("accessToken", true));

        // Verify user was created in database
        var users = userRepository.findAll();
        assertThat(users).hasSize(1);

        User user = users.get(0);
        assertThat(user.getProvider()).isEqualTo(User.AuthProvider.ANONYMOUS);
        assertThat(user.getProviderId()).isEqualTo("mtg_abc123:철수");
        assertThat(user.getUsername()).isEqualTo("mtg_abc123:철수");
        assertThat(user.getPassword()).isNotNull();
        assertThat(user.getPassword()).startsWith("$2a$"); // BCrypt hash
    }

    @Test
    void shouldLoginExistingAnonymousUserWithCorrectPassword() throws Exception {
        // Given - 기존 유저 생성 (첫 로그인)
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String password = "1234";

        AnonymousLoginRequest request = new AnonymousLoginRequest(username, password);

        // 첫 번째 로그인 (유저 생성)
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(true));

        // When - 두 번째 로그인 (같은 이름, 같은 비밀번호)
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("철수"))
                .andExpect(jsonPath("$.data.isNewUser").value(false))  // 기존 유저
                .andExpect(cookie().exists("accessToken"));

        // Verify no duplicate user was created
        var users = userRepository.findAll();
        assertThat(users).hasSize(1);
    }

    @Test
    void shouldReturn401WhenPasswordIncorrect() throws Exception {
        // Given - 유저 생성
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String correctPassword = "1234";
        String wrongPassword = "wrong";

        AnonymousLoginRequest createRequest = new AnonymousLoginRequest(username, correctPassword);

        // 유저 생성
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // When - 잘못된 비밀번호로 로그인 시도
        AnonymousLoginRequest loginRequest = new AnonymousLoginRequest(username, wrongPassword);

        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldAllowSameUsernameInDifferentMeetings() throws Exception {
        // Given
        String username = "철수";
        String password = "1234";

        String meetingCode1 = "mtg_abc123";
        String meetingCode2 = "mtg_xyz789";

        AnonymousLoginRequest request = new AnonymousLoginRequest(username, password);

        // When - Meeting A에서 "철수" 생성
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("철수"))
                .andExpect(jsonPath("$.data.isNewUser").value(true));

        // When - Meeting B에서도 "철수" 생성 (가능해야 함)
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("철수"))
                .andExpect(jsonPath("$.data.isNewUser").value(true));  // 새 유저

        // Verify 두 개의 별도 User 레코드 생성됨
        var users = userRepository.findAll();
        assertThat(users).hasSize(2);

        assertThat(users).extracting(User::getProviderId)
                .containsExactlyInAnyOrder(
                        "mtg_abc123:철수",
                        "mtg_xyz789:철수"
                );
    }

    @Test
    void shouldPreventUsernameCollisionInSameMeeting() throws Exception {
        // Given - Meeting에서 "철수"가 이미 참여
        String meetingCode = "mtg_abc123";
        String username = "철수";
        String password1 = "1234";
        String password2 = "5678";

        AnonymousLoginRequest request1 = new AnonymousLoginRequest(username, password1);

        // 첫 번째 "철수" 생성
        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(true));

        // When - 다른 비밀번호로 같은 이름 시도
        AnonymousLoginRequest request2 = new AnonymousLoginRequest(username, password2);

        mockMvc.perform(post("/api/v1/meetings/{meetingCode}/auth/anonymous", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnauthorized())  // 비밀번호 불일치
                .andExpect(jsonPath("$.success").value(false));

        // Verify 여전히 1명의 유저만 존재
        var users = userRepository.findAll();
        assertThat(users).hasSize(1);
    }
}


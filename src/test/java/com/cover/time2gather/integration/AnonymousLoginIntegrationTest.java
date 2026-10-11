package com.cover.time2gather.integration;

import com.cover.time2gather.api.meeting.dto.request.AnonymousLoginRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.Cookie;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AnonymousLoginIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreatePasswordlessGlobalAnonymousSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/anonymous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnonymousLoginRequest("철수"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("철수"))
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(cookie().exists("__session"))
                .andExpect(cookie().httpOnly("__session", true))
                .andExpect(cookie().secure("__session", true));

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.getFirst().getProvider()).isEqualTo(User.AuthProvider.ANONYMOUS);
        assertThat(users.getFirst().getProfileImageUrl()).isEmpty();
        assertThat(users.getFirst().getProviderId()).isNotBlank();
    }

    @Test
    void shouldTreatDuplicateDisplayNamesAsDifferentPeople() throws Exception {
        AnonymousLoginRequest request = new AnonymousLoginRequest("철수");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/anonymous")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(2)
                .extracting(User::getUsername)
                .containsOnly("철수");
        assertThat(users).extracting(User::getProviderId).doesNotHaveDuplicates();
    }

    @Test
    void shouldRejectBlankOrOversizedDisplayName() throws Exception {
        mockMvc.perform(post("/api/v1/auth/anonymous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/anonymous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnonymousLoginRequest("x".repeat(101)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRenameAnonymousUserWithoutChangingIdentity() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/anonymous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"기존 이름\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie sessionCookie = loginResult.getResponse().getCookie("__session");
        Long originalUserId = userRepository.findAll().getFirst().getId();

        mockMvc.perform(put("/api/v1/auth/me")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\" 바꾼 이름 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(originalUserId))
                .andExpect(jsonPath("$.data.username").value("바꾼 이름"))
                .andExpect(jsonPath("$.data.isNewUser").value(false))
                .andExpect(cookie().exists("__session"));

        assertThat(userRepository.findAll()).hasSize(1);
        assertThat(userRepository.findById(originalUserId).orElseThrow().getUsername())
                .isEqualTo("바꾼 이름");
    }
}

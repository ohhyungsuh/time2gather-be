package com.cover.time2gather.api.admin;

import com.cover.time2gather.domain.admin.AdminDashboardService;
import com.cover.time2gather.domain.admin.AdminSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @Test
    void shouldRejectUnauthenticatedAdminRequests() throws Exception {
        mockMvc.perform(get("/api/v1/admin/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectNormalUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/summary"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnSummaryToAdmins() throws Exception {
        when(adminDashboardService.getSummary()).thenReturn(new AdminSummary(
                new AdminSummary.UserStats(10L, 8L, 2L, 1L),
                new AdminSummary.MeetingStats(12L, 9L, 3L)
        ));

        mockMvc.perform(get("/api/v1/admin/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users.total").value(10))
                .andExpect(jsonPath("$.data.meetings.active").value(9));
    }

}

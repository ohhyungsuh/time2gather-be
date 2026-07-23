package com.cover.time2gather.api.admin;

import com.cover.time2gather.api.admin.dto.AdminMeetingResponse;
import com.cover.time2gather.api.admin.dto.AdminPageResponse;
import com.cover.time2gather.api.admin.dto.AdminSummaryResponse;
import com.cover.time2gather.api.admin.dto.AdminUserResponse;
import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.domain.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    public ApiResponse<AdminSummaryResponse> getSummary() {
        return ApiResponse.success(AdminSummaryResponse.from(adminDashboardService.getSummary()));
    }

    @GetMapping("/users")
    public ApiResponse<AdminPageResponse<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(AdminPageResponse.from(
                adminDashboardService.searchUsers(query, page, size),
                AdminUserResponse::from
        ));
    }

    @GetMapping("/meetings")
    public ApiResponse<AdminPageResponse<AdminMeetingResponse>> getMeetings(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(AdminPageResponse.from(
                adminDashboardService.searchMeetings(query, page, size),
                AdminMeetingResponse::from
        ));
    }
}

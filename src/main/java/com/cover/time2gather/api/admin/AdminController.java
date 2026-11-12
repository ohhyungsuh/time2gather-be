package com.cover.time2gather.api.admin;

import com.cover.time2gather.api.admin.dto.AdminMeetingResponse;
import com.cover.time2gather.api.admin.dto.AdminPageResponse;
import com.cover.time2gather.api.admin.dto.AdminSummaryResponse;
import com.cover.time2gather.api.admin.dto.AdminUserResponse;
import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.domain.admin.AdminDashboardService;
import com.cover.time2gather.domain.admin.AdminUsageService;
import com.cover.time2gather.domain.admin.AdminUsageStats;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import java.util.Set;
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
    private final AdminUsageService adminUsageService;

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<AdminUsageStats>> getUsage(
            @RequestParam(required = false) Set<Long> excludeHostUserIds
    ) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(
                adminUsageService.getUsage(excludeHostUserIds == null ? Set.of() : excludeHostUserIds)));
    }

    @GetMapping("/summary")
    public ApiResponse<AdminSummaryResponse> getSummary() {
        return ApiResponse.success(AdminSummaryResponse.from(adminDashboardService.getSummary()));
    }

    @GetMapping("/users")
    public ApiResponse<AdminPageResponse<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor
    ) {
        return ApiResponse.success(AdminPageResponse.from(
                cursor == null ? adminDashboardService.searchUsers(query, page, size)
                        : adminDashboardService.searchUsers(query, page, size, cursor),
                AdminUserResponse::from
        ));
    }

    @GetMapping("/meetings")
    public ApiResponse<AdminPageResponse<AdminMeetingResponse>> getMeetings(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "false") boolean confirmedOnly
    ) {
        return ApiResponse.success(AdminPageResponse.from(
                adminDashboardService.searchMeetings(query, page, size, cursor, confirmedOnly),
                AdminMeetingResponse::from
        ));
    }
}

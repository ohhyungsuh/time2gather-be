package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.config.security.CurrentUser;
import com.cover.time2gather.domain.identity.MeetingNameService;
import com.cover.time2gather.domain.user.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/meetings/{code}/profile") @RequiredArgsConstructor
public class MeetingProfileController {
    private final MeetingNameService names;
    public record Profile(String username) { }
    public record ProfileRequest(
            @NotBlank(message = "{validation.username.required}") @Size(max = 100) String username,
            boolean onlyIfAbsent) { }
    @ModelAttribute void noCache(HttpServletResponse response) { response.setHeader("Cache-Control", "private, no-store"); }
    @GetMapping public ApiResponse<Profile> get(@PathVariable String code, @CurrentUser User user) {
        return ApiResponse.success(new Profile(names.get(code, user)));
    }
    @PutMapping public ApiResponse<Profile> save(@PathVariable String code, @CurrentUser User user, @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.success(new Profile(names.save(code, user, request.username(), request.onlyIfAbsent())));
    }
}

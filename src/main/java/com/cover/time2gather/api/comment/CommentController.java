package com.cover.time2gather.api.comment;

import com.cover.time2gather.api.common.ApiResponse;
import com.cover.time2gather.config.security.JwtAuthentication;
import com.cover.time2gather.domain.comment.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/meetings/{code}/comments") @RequiredArgsConstructor
public class CommentController {
    private final CommentService service;
    @ModelAttribute void noCache(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
    }
    private Long actor(JwtAuthentication auth) { return auth == null ? null : auth.userId(); }
    @GetMapping public ApiResponse<CommentService.Page> list(@PathVariable String code, @AuthenticationPrincipal JwtAuthentication auth,
            @RequestParam(required = false) String rootId, @RequestParam(required = false) String cursor) {
        return ApiResponse.success(service.page(code, actor(auth), rootId, cursor));
    }
    @GetMapping("/summary") public ApiResponse<CommentService.Summary> summary(@PathVariable String code, @AuthenticationPrincipal JwtAuthentication auth) {
        return ApiResponse.success(service.summary(code, actor(auth)));
    }
    @GetMapping("/activity") public ApiResponse<List<CommentService.Activity>> activity(@PathVariable String code,
            @AuthenticationPrincipal JwtAuthentication auth, @RequestParam(defaultValue = "0") long after) {
        return ApiResponse.success(service.activity(code, actor(auth), Math.max(0, after)));
    }
    @GetMapping("/people") public ApiResponse<List<CommentService.Person>> people(@PathVariable String code) {
        return ApiResponse.success(service.candidates(code));
    }
    @GetMapping("/{id}") public ApiResponse<CommentService.View> one(@PathVariable String code, @PathVariable String id, @AuthenticationPrincipal JwtAuthentication auth) {
        return ApiResponse.success(service.one(code, actor(auth), id));
    }
    @PostMapping public ApiResponse<CommentService.View> create(@PathVariable String code, @AuthenticationPrincipal JwtAuthentication auth, @RequestBody CommentService.Write body) {
        return ApiResponse.success(service.create(code, actor(auth), body));
    }
    @PutMapping("/{id}") public ApiResponse<CommentService.View> edit(@PathVariable String code, @PathVariable String id,
            @AuthenticationPrincipal JwtAuthentication auth, @RequestBody CommentService.Write body) {
        return ApiResponse.success(service.edit(code, actor(auth), id, body));
    }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable String code, @PathVariable String id, @AuthenticationPrincipal JwtAuthentication auth) {
        service.remove(code, actor(auth), id, false); return ApiResponse.success(null);
    }
    @PutMapping("/{id}/reactions") public ApiResponse<CommentService.View> react(@PathVariable String code, @PathVariable String id,
            @AuthenticationPrincipal JwtAuthentication auth, @RequestBody CommentService.ReactionWrite body) {
        return ApiResponse.success(service.react(code, actor(auth), id, body));
    }
    @PostMapping("/{id}/hide") public ApiResponse<Void> hide(@PathVariable String code, @PathVariable String id, @AuthenticationPrincipal JwtAuthentication auth) {
        service.remove(code, actor(auth), id, true); return ApiResponse.success(null);
    }
}

package com.cover.time2gather.api.admin.dto;

import com.cover.time2gather.domain.admin.AdminPage;

import java.util.List;
import java.util.function.Function;

public record AdminPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <S, T> AdminPageResponse<T> from(AdminPage<S> source, Function<S, T> mapper) {
        return new AdminPageResponse<>(
                source.content().stream().map(mapper).toList(),
                source.page(),
                source.size(),
                source.totalElements(),
                source.totalPages()
        );
    }
}

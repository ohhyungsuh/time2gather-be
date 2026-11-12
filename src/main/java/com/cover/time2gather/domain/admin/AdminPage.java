package com.cover.time2gather.domain.admin;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String nextCursor
) {
    public AdminPage(List<T> content, int page, int size, long totalElements, int totalPages) {
        this(content, page, size, totalElements, totalPages, null);
    }

    public static <T> AdminPage<T> from(Page<T> source) {
        return new AdminPage<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source instanceof com.cover.time2gather.domain.common.CursorPage<?> cursorPage
                        ? cursorPage.getNextCursor() : null
        );
    }
}

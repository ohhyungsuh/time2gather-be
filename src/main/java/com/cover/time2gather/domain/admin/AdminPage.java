package com.cover.time2gather.domain.admin;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> AdminPage<T> from(Page<T> source) {
        return new AdminPage<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages()
        );
    }
}

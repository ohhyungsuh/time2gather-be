package com.cover.time2gather.domain.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Function;

/** Page-number compatibility with an optional bounded-read continuation. */
public class CursorPage<T> extends PageImpl<T> {
    private final String nextCursor;

    public CursorPage(List<T> content, Pageable pageable, long total, String nextCursor) {
        super(content, pageable, total);
        this.nextCursor = nextCursor;
    }

    public String getNextCursor() { return nextCursor; }

    @Override
    public <U> Page<U> map(Function<? super T, ? extends U> converter) {
        return new CursorPage<>(getConvertedContent(converter), getPageable(), getTotalElements(), nextCursor);
    }
}

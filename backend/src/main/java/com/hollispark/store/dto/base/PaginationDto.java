package com.hollispark.store.dto.base;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** One page of a list, with what a table needs to draw its pager. */
public record PaginationDto<T>(List<T> content, int pageNumber, int pageSize, long totalElements, int totalPages,
                               boolean last) {

    public static <E, T> PaginationDto<T> of(Page<E> page, Function<E, T> map) {
        return new PaginationDto<>(page.getContent().stream().map(map).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}

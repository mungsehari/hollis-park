package com.hollispark.store.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Turns the page/size query parameters into a safe PageRequest (no negative page, size capped). */
public final class PageRequests {
    private PageRequests() {}

    public static PageRequest newestFirst(int page, int size, int maxSize) {
        return PageRequest.of(Math.max(0, page), Math.min(maxSize, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}

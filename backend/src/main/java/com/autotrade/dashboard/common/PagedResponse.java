package com.autotrade.dashboard.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A generic page-of-results wire shape (E6-F3-S3, this codebase's first true paginated endpoint --
 * every earlier "list" endpoint is limit-only, always page 0). Wraps Spring Data's {@link Page}
 * rather than returning it directly over HTTP, since {@code Page}'s own JSON serialization isn't a
 * stable, intentional wire contract.
 */
public record PagedResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}

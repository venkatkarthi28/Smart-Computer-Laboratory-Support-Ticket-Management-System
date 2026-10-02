package com.example.labsupport.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Our own simple wrapper for paginated lists, so the JSON shape stays stable
 * and the frontend only needs to know these 7 fields.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    /** Convert a Spring Data Page (already mapped to DTOs) into our wrapper. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
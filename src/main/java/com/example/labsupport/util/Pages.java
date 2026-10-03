package com.example.labsupport.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pages {

    private Pages() {
    }

    /** Safe Pageable: page >= 0, size between 1 and 100, no sorting. */
    public static Pageable of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    /** Same, but sorted (the entity field name is used, e.g. "createdAt"). */
    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
    }
}

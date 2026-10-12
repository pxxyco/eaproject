package it.unical.ea_project.util;

import org.springframework.data.domain.PageRequest;

public final class PageUtils {
    public static final int MAX_PAGE_SIZE = 50;
    public static final int MAX_SUGGESTIONS = 10;

    private PageUtils() {}

    public static PageRequest of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    public static PageRequest suggestions(int limit) {
        return PageRequest.of(0, Math.clamp(limit, 1, MAX_SUGGESTIONS));
    }
}
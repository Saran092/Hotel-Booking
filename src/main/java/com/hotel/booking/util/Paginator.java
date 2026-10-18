package com.hotel.booking.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Sorting and pagination for list screens. The data sets of a single hotel are small, so the
 * lists are filtered in memory and then sliced into pages.
 */
public final class Paginator {

    private Paginator() {
    }

    public static <T> PageResult<T> paginate(List<T> source, Map<String, Comparator<T>> sortable, String sort, String dir,
                                             int page, int size) {
        List<T> list = new ArrayList<>(source);
        Comparator<T> comparator = sort == null ? null : sortable.get(sort);
        if (comparator != null) {
            list.sort("desc".equalsIgnoreCase(dir) ? comparator.reversed() : comparator);
        }
        int total = list.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) size));
        int current = Math.min(Math.max(page, 1), totalPages);
        int from = (current - 1) * size;
        int to = Math.min(from + size, total);
        return new PageResult<>(from >= total ? List.of() : list.subList(from, to), current, size, totalPages, total);
    }
}

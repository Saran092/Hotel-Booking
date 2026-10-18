package com.hotel.booking.util;

import java.util.List;

/** One page of a sorted / filtered list plus the numbers needed to draw the pager. */
public class PageResult<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final int totalPages;
    private final long totalElements;

    public PageResult(List<T> items, int page, int size, int totalPages, long totalElements) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getTotalPages() { return totalPages; }
    public long getTotalElements() { return totalElements; }
    public boolean isEmpty() { return items.isEmpty(); }
    public boolean isHasPrevious() { return page > 1; }
    public boolean isHasNext() { return page < totalPages; }
}

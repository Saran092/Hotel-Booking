package com.hotel.booking.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Builds the links for the pager and the sortable column headers so that every active
 * search / filter parameter is preserved when the user moves to another page or re-sorts.
 */
public class ListQuery {

    private final String path;
    private final Map<String, String[]> params;
    private final String sort;
    private final String dir;

    public ListQuery(HttpServletRequest request) {
        this.path = request.getRequestURI();
        this.params = request.getParameterMap();
        this.sort = request.getParameter("sort");
        this.dir = "desc".equalsIgnoreCase(request.getParameter("dir")) ? "desc" : "asc";
    }

    private UriComponentsBuilder base(String... skip) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath(path);
        outer:
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            for (String s : skip) {
                if (e.getKey().equals(s)) continue outer;
            }
            for (String v : e.getValue()) {
                if (v != null && !v.isBlank()) b.queryParam(e.getKey(), v);
            }
        }
        return b;
    }

    public String pageUrl(int page) {
        return base("page").queryParam("page", page).build().encode().toUriString();
    }

    public String sortUrl(String column) {
        String newDir = column.equals(sort) && "asc".equals(dir) ? "desc" : "asc";
        return base("page", "sort", "dir").queryParam("sort", column).queryParam("dir", newDir).build().encode().toUriString();
    }

    public String indicator(String column) {
        if (!column.equals(sort)) return "";
        return "asc".equals(dir) ? " ▲" : " ▼";
    }

    public String getResetUrl() { return path; }
}

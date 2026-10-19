package com.hotel.booking.api;

import com.hotel.booking.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardApi {

    private final DashboardService dashboard;

    public AdminDashboardApi(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/home")
    public Map<String, Object> home() {
        return Map.of("stats", dashboard.adminStats());
    }

    @GetMapping("/reports")
    public Map<String, Object> reports() {
        return Map.of("stats", dashboard.reports());
    }
}

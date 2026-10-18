package com.hotel.booking.controller;

import com.hotel.booking.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** US014: admin home page and dashboard overview. */
@Controller
public class AdminHomeController extends BaseController {

    private final DashboardService dashboard;

    public AdminHomeController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/admin/home")
    public String home(Model model) {
        model.addAttribute("stats", dashboard.adminStats());
        return "admin/home";
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("stats", dashboard.reports());
        return "admin/reports";
    }
}

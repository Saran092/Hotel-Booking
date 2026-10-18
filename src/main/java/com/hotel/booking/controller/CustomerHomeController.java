package com.hotel.booking.controller;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.service.BookingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** US003: customer home page with a personalised welcome and the navigation menu. */
@Controller
public class CustomerHomeController extends BaseController {

    private final BookingService bookingService;

    public CustomerHomeController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/customer/home")
    public String home(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        var upcoming = bookingService.history(me(principal)).stream().filter(b -> b.isUpcoming()).count();
        model.addAttribute("upcomingCount", upcoming);
        return "customer/home";
    }
}

package com.hotel.booking.controller;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.model.User;
import com.hotel.booking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Helpers shared by all controllers. */
public abstract class BaseController {

    @Autowired
    protected UserService userService;

    /** The logged-in user as a fresh entity. */
    protected User me(AppUserDetails principal) {
        return userService.get(principal.getId());
    }

    protected static void success(RedirectAttributes ra, String message) {
        ra.addFlashAttribute("success", message);
    }

    protected static void error(RedirectAttributes ra, String message) {
        ra.addFlashAttribute("error", message);
    }
}

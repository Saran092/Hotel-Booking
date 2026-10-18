package com.hotel.booking.controller;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.form.ProfileForm;
import com.hotel.booking.model.User;
import com.hotel.booking.service.NotificationService;
import com.hotel.booking.util.ValidationGroups;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** "My Profile" for every role (US003 / US013), plus the notification inbox. */
@Controller
public class ProfileController extends BaseController {

    private final NotificationService notifications;

    public ProfileController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile/view";
    }

    @GetMapping("/profile/edit")
    public String editForm(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        User u = me(principal);
        ProfileForm f = new ProfileForm();
        f.setFullName(u.getFullName());
        f.setEmail(u.getEmail());
        f.setCountryCode(u.getCountryCode() == null ? "+91" : u.getCountryCode());
        f.setMobile(u.getMobile());
        f.setAddress(u.getAddress());
        model.addAttribute("form", f);
        return "profile/edit";
    }

    @PostMapping("/profile/edit")
    public String edit(@AuthenticationPrincipal AppUserDetails principal,
                       @Validated(ValidationGroups.Ordered.class) @ModelAttribute("form") ProfileForm form,
                       BindingResult errors, RedirectAttributes ra) {
        User u = me(principal);
        userService.validateProfile(u, form, errors);
        if (errors.hasErrors()) return "profile/edit";
        userService.updateProfile(u, form);
        success(ra, "Your profile has been updated successfully.");
        return "redirect:/profile";
    }

    @GetMapping("/notifications")
    public String inbox(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        User u = me(principal);
        model.addAttribute("notifications", notifications.latest(u));
        notifications.markAllRead(u);
        return "profile/notifications";
    }
}

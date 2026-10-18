package com.hotel.booking.controller;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.config.LoginSuccessHandler;
import com.hotel.booking.form.ChangePasswordForm;
import com.hotel.booking.form.ForgotPasswordForm;
import com.hotel.booking.form.RegistrationForm;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Home redirect, registration, login page, forgotten and forced password change. */
@Controller
public class AuthController extends BaseController {

    @GetMapping("/")
    public String root(@AuthenticationPrincipal AppUserDetails principal) {
        if (principal == null) return "redirect:/login";
        return "redirect:" + LoginSuccessHandler.landingPage(false, principal.getPrimaryRole());
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AppUserDetails principal) {
        if (principal != null) return "redirect:/";
        return "auth/login";
    }

    // ------------------------------------------------------------------ US001 registration

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult errors, RedirectAttributes ra) {
        userService.validateRegistration(form, errors);
        if (errors.hasErrors()) return "auth/register";
        userService.register(form);
        success(ra, "Registration successful! Please log in with your username and password.");
        return "redirect:/login";
    }

    // ------------------------------------------------------------------ forgot password

    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        model.addAttribute("form", new ForgotPasswordForm());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgot(@Valid @ModelAttribute("form") ForgotPasswordForm form, BindingResult errors, RedirectAttributes ra) {
        if (!form.getNewPassword().equals(form.getConfirmPassword()) && !errors.hasFieldErrors("confirmPassword")) {
            errors.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        if (errors.hasErrors()) return "auth/forgot-password";
        if (!userService.resetForgottenPassword(form)) {
            errors.reject("nomatch", "We could not find an active account with that username and e-mail address.");
            return "auth/forgot-password";
        }
        success(ra, "Your password has been reset. Please log in with the new password.");
        return "redirect:/login";
    }

    // ------------------------------------------------------------------ change password (also forced after admin reset)

    @GetMapping("/change-password")
    public String changeForm(Model model, @AuthenticationPrincipal AppUserDetails principal) {
        model.addAttribute("form", new ChangePasswordForm());
        model.addAttribute("forced", me(principal).isMustChangePassword());
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String change(@Valid @ModelAttribute("form") ChangePasswordForm form, BindingResult errors, Model model,
                         @AuthenticationPrincipal AppUserDetails principal, RedirectAttributes ra) {
        var user = me(principal);
        if (!form.getNewPassword().equals(form.getConfirmPassword()) && !errors.hasFieldErrors("confirmPassword")) {
            errors.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        if (errors.hasErrors()) {
            model.addAttribute("forced", user.isMustChangePassword());
            return "auth/change-password";
        }
        userService.changePassword(user, form.getNewPassword());
        success(ra, "Your password has been changed.");
        return "redirect:" + LoginSuccessHandler.landingPage(false, principal.getPrimaryRole());
    }
}

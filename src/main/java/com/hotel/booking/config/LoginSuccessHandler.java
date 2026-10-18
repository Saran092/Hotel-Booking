package com.hotel.booking.config;

import com.hotel.booking.model.Role;
import com.hotel.booking.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Clears the failed-login counter and sends every role to its own landing page. */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository users;

    public LoginSuccessHandler(UserRepository users) {
        this.users = users;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        AppUserDetails details = (AppUserDetails) authentication.getPrincipal();
        var user = users.findById(details.getId()).orElseThrow();
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        users.save(user);
        response.sendRedirect(request.getContextPath() + landingPage(user.isMustChangePassword(), details.getPrimaryRole()));
    }

    public static String landingPage(boolean mustChangePassword, Role role) {
        if (mustChangePassword) return "/change-password";
        return switch (role) {
            case ADMIN -> "/admin/home";
            case STAFF -> "/staff/complaints";
            default -> "/customer/home";
        };
    }
}

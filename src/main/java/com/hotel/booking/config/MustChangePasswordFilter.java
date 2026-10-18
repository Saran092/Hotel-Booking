package com.hotel.booking.config;

import com.hotel.booking.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Users created or reset by an admin must choose their own password before they can use the application. */
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public MustChangePasswordFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppUserDetails details) {
            String path = request.getRequestURI();
            boolean exempt = path.equals("/change-password") || path.equals("/logout") || path.startsWith("/css/")
                    || path.startsWith("/js/") || path.startsWith("/error");
            if (!exempt && users.findById(details.getId()).map(u -> u.isMustChangePassword()).orElse(false)) {
                response.sendRedirect(request.getContextPath() + "/change-password");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

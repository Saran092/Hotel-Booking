package com.hotel.booking.security;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/** Users created or reset by an admin must choose their own password before using the API. */
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT = Set.of(
            "/api/auth/change-password", "/api/auth/logout", "/api/auth/me", "/api/meta");

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
            if (!EXEMPT.contains(path) && users.findById(details.getId()).map(u -> u.isMustChangePassword()).orElse(false)) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"You must change your password before continuing.\",\"code\":\"MUST_CHANGE_PASSWORD\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

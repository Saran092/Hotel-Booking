package com.hotel.booking.config;

import com.hotel.booking.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/** Counts failed logins and locks the account after too many attempts. */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final UserRepository users;
    private final HotelProperties props;

    public LoginFailureHandler(UserRepository users, HotelProperties props) {
        this.users = users;
        this.props = props;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        String code = "invalid";
        if (ex instanceof LockedException) {
            code = "locked";
        } else if (ex instanceof DisabledException) {
            code = "disabled";
        } else if (ex instanceof BadCredentialsException) {
            String username = request.getParameter("username");
            if (username != null) {
                var found = users.findByUsername(username.trim());
                if (found.isPresent()) {
                    var user = found.get();
                    user.setFailedAttempts(user.getFailedAttempts() + 1);
                    if (user.getFailedAttempts() >= props.getMaxFailedLogins()) {
                        user.setLockedUntil(LocalDateTime.now().plusMinutes(props.getLockMinutes()));
                        user.setFailedAttempts(0);
                        code = "locked";
                    }
                    users.save(user);
                }
            }
        }
        response.sendRedirect(request.getContextPath() + "/login?error=" + code);
    }
}

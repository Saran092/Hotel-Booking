package com.hotel.booking.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns a JSON 401 instead of redirecting to a login page. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Please log in to continue.\",\"code\":\"UNAUTHORIZED\"}");
    }
}

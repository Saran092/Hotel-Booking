package com.hotel.booking.config;

import com.hotel.booking.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Passwords are stored as salted BCrypt hashes, never in clear text. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LoginSuccessHandler success, LoginFailureHandler failure,
                                           UserRepository users) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/css/**", "/js/**", "/images/**", "/login", "/register", "/forgot-password", "/error", "/error/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(success)
                .failureHandler(failure)
                .permitAll())
            .logout(l -> l
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .addFilterBefore(new MustChangePasswordFilter(users), AuthorizationFilter.class);
        return http.build();
    }
}

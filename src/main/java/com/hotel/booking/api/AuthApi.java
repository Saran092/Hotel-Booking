package com.hotel.booking.api;

import com.hotel.booking.api.dto.LoginRequest;
import com.hotel.booking.api.dto.LoginResponse;
import com.hotel.booking.api.dto.UserSummary;
import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.form.ChangePasswordForm;
import com.hotel.booking.form.ForgotPasswordForm;
import com.hotel.booking.form.RegistrationForm;
import com.hotel.booking.model.User;
import com.hotel.booking.repository.UserRepository;
import com.hotel.booking.security.JwtService;
import com.hotel.booking.security.RestExceptionHandler;
import com.hotel.booking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/** Login, registration, forgotten and forced password change for the SPA. */
@RestController
@RequestMapping("/api/auth")
public class AuthApi {

    private final UserRepository users;
    private final UserService userService;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final HotelProperties props;
    private final RestExceptionHandler forms;

    public AuthApi(UserRepository users, UserService userService, PasswordEncoder encoder,
                   JwtService jwt, HotelProperties props, RestExceptionHandler forms) {
        this.users = users;
        this.userService = userService;
        this.encoder = encoder;
        this.jwt = jwt;
        this.props = props;
        this.forms = forms;
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var found = users.findByUsername(req.username() == null ? "" : req.username().trim());
        if (found.isEmpty()) {
            return unauthorized("Invalid username or password.", "invalid");
        }
        User user = found.get();
        if (!user.isActive()) {
            return unauthorized("This account has been deactivated.", "disabled");
        }
        if (user.isLocked()) {
            return unauthorized(lockedMessage(user), "locked");
        }
        if (req.password() == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            boolean lockedNow = false;
            if (user.getFailedAttempts() >= props.getMaxFailedLogins()) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(props.getLockMinutes()));
                user.setFailedAttempts(0);
                lockedNow = true;
            }
            users.save(user);
            if (lockedNow) return unauthorized(lockedMessage(user), "locked");
            return unauthorized("Invalid username or password.", "invalid");
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        users.save(user);
        String token = jwt.issue(user.getId(), user.getUsername());
        return ResponseEntity.ok(new LoginResponse(token, UserSummary.of(user)));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationForm form) {
        var result = forms.validate(form);
        userService.validateRegistration(form, result);
        RestExceptionHandler.throwIfInvalid(result);
        userService.register(form);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful! Please log in with your username and password."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@Valid @RequestBody ForgotPasswordForm form) {
        var result = forms.validate(form);
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        RestExceptionHandler.throwIfInvalid(result);
        if (!userService.resetForgottenPassword(form)) {
            throw new com.hotel.booking.service.BusinessException(
                    "We could not find an active account with that username and e-mail address.");
        }
        return ResponseEntity.ok(Map.of("message", "Your password has been reset. Please log in with the new password."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> change(@Valid @RequestBody ChangePasswordForm form,
                                    @AuthenticationPrincipal AppUserDetails principal) {
        User user = userService.get(principal.getId());
        var result = forms.validate(form);
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        RestExceptionHandler.throwIfInvalid(result);
        userService.changePassword(user, form.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Your password has been changed."));
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AppUserDetails principal) {
        return UserSummary.of(userService.get(principal.getId()));
    }

    /** Stateless: the client simply discards the token. */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    private static ResponseEntity<Map<String, Object>> unauthorized(String message, String code) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", message, "code", code));
    }

    private String lockedMessage(User user) {
        if (user.getLockedUntil() == null || !user.isLocked()) return "Your account is locked. Try again later.";
        long minutes = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
        return "Your account is locked after too many failed attempts. Try again in " + minutes + " minute(s).";
    }
}

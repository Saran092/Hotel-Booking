package com.hotel.booking.service;

import com.hotel.booking.form.*;
import com.hotel.booking.model.Role;
import com.hotel.booking.model.StaffExpertise;
import com.hotel.booking.model.User;
import com.hotel.booking.repository.UserRepository;
import com.hotel.booking.util.Patterns;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final NotificationService notifications;

    public UserService(UserRepository users, PasswordEncoder encoder, NotificationService notifications) {
        this.users = users;
        this.encoder = encoder;
        this.notifications = notifications;
    }

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new BusinessException("User not found."));
    }

    // ------------------------------------------------------------------ registration (US001)

    public void validateRegistration(RegistrationForm f, BindingResult errors) {
        if (!f.getPassword().equals(f.getConfirmPassword()) && !errors.hasFieldErrors("confirmPassword")) {
            errors.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        if (!errors.hasFieldErrors("email") && users.existsByEmailIgnoreCase(f.getEmail().trim())) {
            errors.rejectValue("email", "duplicate", "Email already registered.");
        }
        if (!errors.hasFieldErrors("mobile") && users.existsByCountryCodeAndMobile(f.getCountryCode(), f.getMobile())) {
            errors.rejectValue("mobile", "duplicate", "Mobile number already registered.");
        }
        if (!errors.hasFieldErrors("username") && users.existsByUsernameIgnoreCase(f.getUsername())) {
            errors.rejectValue("username", "duplicate", Patterns.MSG_USERNAME);
        }
    }

    @Transactional
    public User register(RegistrationForm f) {
        User u = new User();
        u.setFullName(f.getFullName().trim().replaceAll("\\s+", " "));
        u.setEmail(f.getEmail().trim().toLowerCase(Locale.ROOT));
        u.setCountryCode(f.getCountryCode());
        u.setMobile(f.getMobile());
        u.setAddress(f.getAddress().trim());
        u.setUsername(f.getUsername());
        u.setPasswordHash(encoder.encode(f.getPassword()));
        u.getRoles().add(Role.CUSTOMER);
        return users.save(u);
    }

    // ------------------------------------------------------------------ profile (US013)

    public void validateProfile(User current, ProfileForm f, BindingResult errors) {
        if (!errors.hasFieldErrors("email") && users.existsByEmailIgnoreCaseAndIdNot(f.getEmail().trim(), current.getId())) {
            errors.rejectValue("email", "duplicate", "Email already registered.");
        }
        if (!errors.hasFieldErrors("mobile") && users.existsByCountryCodeAndMobileAndIdNot(f.getCountryCode(), f.getMobile(), current.getId())) {
            errors.rejectValue("mobile", "duplicate", "Mobile number already registered.");
        }
    }

    @Transactional
    public User updateProfile(User u, ProfileForm f) {
        u.setFullName(f.getFullName().trim().replaceAll("\\s+", " "));
        u.setEmail(f.getEmail().trim().toLowerCase(Locale.ROOT));
        u.setCountryCode(f.getCountryCode());
        u.setMobile(f.getMobile());
        u.setAddress(StringUtils.hasText(f.getAddress()) ? f.getAddress().trim() : null);
        return users.save(u);
    }

    // ------------------------------------------------------------------ passwords

    @Transactional
    public void changePassword(User u, String rawPassword) {
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setMustChangePassword(false);
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        users.save(u);
    }

    /** "Forgot password": the account is identified by username + registered e-mail. */
    @Transactional
    public boolean resetForgottenPassword(ForgotPasswordForm f) {
        return users.findByUsernameIgnoreCaseAndEmailIgnoreCase(f.getUsername().trim(), f.getEmail().trim()).map(u -> {
            if (!u.isActive()) return false;
            changePassword(u, f.getNewPassword());
            notifications.notify(u, "Your password was changed. If this was not you, please contact support.");
            return true;
        }).orElse(false);
    }

    /** Random password that satisfies the password policy. */
    public static String generatePassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ", lower = "abcdefghijkmnpqrstuvwxyz", digits = "23456789", special = "@#$%&*!?";
        List<Character> chars = new ArrayList<>();
        chars.add(upper.charAt(RANDOM.nextInt(upper.length())));
        chars.add(lower.charAt(RANDOM.nextInt(lower.length())));
        chars.add(digits.charAt(RANDOM.nextInt(digits.length())));
        chars.add(special.charAt(RANDOM.nextInt(special.length())));
        String all = upper + lower + digits + special;
        while (chars.size() < 10) chars.add(all.charAt(RANDOM.nextInt(all.length())));
        java.util.Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        chars.forEach(sb::append);
        return sb.toString();
    }

    // ------------------------------------------------------------------ admin user management (US019)

    public void validateAdminUser(AdminUserForm f, Long editingId, BindingResult errors) {
        if (editingId == null) {
            if (!errors.hasFieldErrors("username") && users.existsByUsernameIgnoreCase(f.getUsername())) {
                errors.rejectValue("username", "duplicate", "Username already exists.");
            }
            if (!errors.hasFieldErrors("email") && users.existsByEmailIgnoreCase(f.getEmail().trim())) {
                errors.rejectValue("email", "duplicate", "Email already registered.");
            }
        } else if (!errors.hasFieldErrors("email") && users.existsByEmailIgnoreCaseAndIdNot(f.getEmail().trim(), editingId)) {
            errors.rejectValue("email", "duplicate", "Email already registered.");
        }
        if (f.getRoles() != null && f.getRoles().contains(Role.STAFF) && f.getExpertise() == null) {
            errors.rejectValue("expertise", "required", "Select the area of expertise for a staff member.");
        }
    }

    /** Creates an account protected by the given one-time password; the user must change it at the first login. */
    @Transactional
    public User createByAdmin(AdminUserForm f, String temp) {
        User u = new User();
        u.setUsername(f.getUsername());
        u.setEmail(f.getEmail().trim().toLowerCase(Locale.ROOT));
        u.setFullName(StringUtils.hasText(f.getFullName()) ? f.getFullName().trim() : f.getUsername());
        u.setRoles(new HashSet<>(f.getRoles()));
        u.setExpertise(f.getRoles().contains(Role.STAFF) ? f.getExpertise() : null);
        u.setPasswordHash(encoder.encode(temp));
        u.setMustChangePassword(true);
        return users.save(u);
    }

    @Transactional
    public User updateByAdmin(Long id, AdminUserForm f, User actingAdmin) {
        User u = get(id);
        boolean self = u.getId().equals(actingAdmin.getId());
        if (self && (!f.getRoles().contains(Role.ADMIN) || !f.isActive())) {
            throw new BusinessException("You cannot remove your own admin role or deactivate your own account.");
        }
        u.setEmail(f.getEmail().trim().toLowerCase(Locale.ROOT));
        if (StringUtils.hasText(f.getFullName())) u.setFullName(f.getFullName().trim());
        u.setRoles(new HashSet<>(f.getRoles()));
        u.setExpertise(f.getRoles().contains(Role.STAFF) ? f.getExpertise() : null);
        u.setActive(f.isActive());
        return users.save(u);
    }

    @Transactional
    public User resetPasswordByAdmin(Long id, String temp) {
        User u = get(id);
        u.setPasswordHash(encoder.encode(temp));
        u.setMustChangePassword(true);
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        return users.save(u);
    }

    @Transactional
    public User setActive(Long id, boolean active, User actingAdmin) {
        User u = get(id);
        if (!active && u.getId().equals(actingAdmin.getId())) {
            throw new BusinessException("You cannot deactivate your own account.");
        }
        u.setActive(active);
        if (active) {
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
        }
        return users.save(u);
    }

    public List<User> search(UserFilter f) {
        String q = f.getQ() == null ? "" : f.getQ().trim().toLowerCase(Locale.ROOT);
        return users.findAll().stream()
                .filter(u -> q.isEmpty() || u.getUsername().toLowerCase(Locale.ROOT).contains(q)
                        || u.getEmail().toLowerCase(Locale.ROOT).contains(q) || u.getFullName().toLowerCase(Locale.ROOT).contains(q))
                .filter(u -> f.getRole() == null || u.hasRole(f.getRole()))
                .filter(u -> f.getStatus() == null || f.getStatus().isBlank()
                        || ("ACTIVE".equals(f.getStatus()) == u.isActive()))
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<User> customers() { return users.findByRole(Role.CUSTOMER); }

    public List<User> staff() { return users.findByRole(Role.STAFF); }

    public boolean hasExpertise(User u, StaffExpertise e) { return u.getExpertise() == e; }
}

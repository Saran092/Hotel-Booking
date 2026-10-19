package com.hotel.booking.api;

import com.hotel.booking.api.dto.UserSummary;
import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.form.AdminUserForm;
import com.hotel.booking.form.UserFilter;
import com.hotel.booking.model.User;
import com.hotel.booking.security.RestExceptionHandler;
import com.hotel.booking.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** US019: user account management for administrators. */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserApi {

    private final UserService userService;
    private final RestExceptionHandler forms;

    public AdminUserApi(UserService userService, RestExceptionHandler forms) {
        this.userService = userService;
        this.forms = forms;
    }

    @GetMapping
    public Map<String, Object> list(@ModelAttribute UserFilter filter) {
        List<User> found = userService.search(filter);
        return Map.of("users", found.stream().map(UserSummary::of).toList());
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody AdminUserForm form) {
        var result = forms.validate(form);
        userService.validateAdminUser(form, null, result);
        RestExceptionHandler.throwIfInvalid(result);
        String tempPassword = UserService.generatePassword();
        User u = userService.createByAdmin(form, tempPassword);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("account", UserSummary.of(u));
        m.put("tempPassword", tempPassword);
        return m;
    }

    @GetMapping("/{id}/edit")
    public Map<String, Object> editForm(@PathVariable Long id) {
        User u = userService.get(id);
        AdminUserForm f = new AdminUserForm();
        f.setUsername(u.getUsername());
        f.setFullName(u.getFullName());
        f.setEmail(u.getEmail());
        f.setRoles(new java.util.HashSet<>(u.getRoles()));
        f.setExpertise(u.getExpertise());
        f.setActive(u.isActive());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("account", UserSummary.of(u));
        m.put("form", f);
        return m;
    }

    @PutMapping("/{id}/edit")
    @Transactional
    public Map<String, Object> edit(@PathVariable Long id, @RequestBody AdminUserForm form,
                                    @AuthenticationPrincipal AppUserDetails principal) {
        User target = userService.get(id);
        form.setUsername(target.getUsername()); // username not editable
        var result = forms.validate(form);
        userService.validateAdminUser(form, id, result);
        RestExceptionHandler.throwIfInvalid(result);
        User u = userService.updateByAdmin(id, form, userService.get(principal.getId()));
        return Map.of("message", "User " + u.getUsername() + " was updated.", "account", UserSummary.of(u));
    }

    @PostMapping("/{id}/reset-password")
    @Transactional
    public Map<String, Object> resetPassword(@PathVariable Long id) {
        String temp = UserService.generatePassword();
        User u = userService.resetPasswordByAdmin(id, temp);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("account", UserSummary.of(u));
        m.put("tempPassword", temp);
        return m;
    }

    @PostMapping("/{id}/status")
    @Transactional
    public Map<String, Object> setStatus(@PathVariable Long id, @RequestParam boolean active,
                                         @AuthenticationPrincipal AppUserDetails principal) {
        User u = userService.setActive(id, active, userService.get(principal.getId()));
        return Map.of("message", "User " + u.getUsername() + " was " + (active ? "reactivated" : "deactivated")
                + ". Status is now " + (active ? "Active" : "Inactive") + ".", "account", UserSummary.of(u));
    }
}

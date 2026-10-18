package com.hotel.booking.controller;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.form.AdminUserForm;
import com.hotel.booking.form.UserFilter;
import com.hotel.booking.model.User;
import com.hotel.booking.service.BusinessException;
import com.hotel.booking.service.UserService;
import com.hotel.booking.util.ListQuery;
import com.hotel.booking.util.PageResult;
import com.hotel.booking.util.Paginator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** US019: user account management for administrators. */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController extends BaseController {

    private static final int PAGE_SIZE = 10;

    private static final Map<String, Comparator<User>> SORTABLE = new HashMap<>();

    static {
        SORTABLE.put("username", Comparator.comparing((User u) -> u.getUsername().toLowerCase()));
        SORTABLE.put("name", Comparator.comparing((User u) -> u.getFullName().toLowerCase()));
        SORTABLE.put("role", Comparator.comparing((User u) -> u.getPrimaryRole().ordinal()));
        SORTABLE.put("status", Comparator.comparing(User::isActive));
    }

    @GetMapping
    public String list(@ModelAttribute("filter") UserFilter filter, @RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String sort, @RequestParam(required = false) String dir,
                       HttpServletRequest request, Model model) {
        List<User> found = userService.search(filter);
        PageResult<User> result = Paginator.paginate(found, SORTABLE, sort, dir, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("lq", new ListQuery(request));
        return "admin/users";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new AdminUserForm());
        return "admin/user-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") AdminUserForm form, BindingResult errors, Model model) {
        userService.validateAdminUser(form, null, errors);
        if (errors.hasErrors()) return "admin/user-form";
        String tempPassword = UserService.generatePassword();
        model.addAttribute("account", userService.createByAdmin(form, tempPassword));
        model.addAttribute("tempPassword", tempPassword);
        model.addAttribute("reset", false);
        return "admin/user-created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User u = userService.get(id);
        AdminUserForm f = new AdminUserForm();
        f.setUsername(u.getUsername());
        f.setFullName(u.getFullName());
        f.setEmail(u.getEmail());
        f.setRoles(new java.util.HashSet<>(u.getRoles()));
        f.setExpertise(u.getExpertise());
        f.setActive(u.isActive());
        model.addAttribute("form", f);
        model.addAttribute("account", u);
        return "admin/user-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("form") AdminUserForm form, BindingResult errors,
                       Model model, @AuthenticationPrincipal AppUserDetails principal, RedirectAttributes ra) {
        User target = userService.get(id);
        // the username is not editable, so it is always taken from the stored account
        form.setUsername(target.getUsername());
        userService.validateAdminUser(form, id, errors);
        if (!errors.hasErrors()) {
            try {
                userService.updateByAdmin(id, form, me(principal));
                success(ra, "User " + target.getUsername() + " was updated.");
                return "redirect:/admin/users";
            } catch (BusinessException e) {
                errors.reject("failed", e.getMessage());
            }
        }
        model.addAttribute("account", target);
        return "admin/user-form";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, Model model) {
        String temp = UserService.generatePassword();
        User u = userService.resetPasswordByAdmin(id, temp);
        model.addAttribute("account", u);
        model.addAttribute("tempPassword", temp);
        model.addAttribute("reset", true);
        return "admin/user-created";
    }

    @PostMapping("/{id}/status")
    public String setStatus(@PathVariable Long id, @RequestParam boolean active, @AuthenticationPrincipal AppUserDetails principal,
                            RedirectAttributes ra) {
        try {
            User u = userService.setActive(id, active, me(principal));
            success(ra, "User " + u.getUsername() + " was " + (active ? "reactivated" : "deactivated")
                    + ". Status is now " + (active ? "Active" : "Inactive") + ".");
        } catch (BusinessException e) {
            error(ra, e.getMessage());
        }
        return "redirect:/admin/users";
    }
}

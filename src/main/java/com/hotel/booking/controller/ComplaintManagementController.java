package com.hotel.booking.controller;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.form.ComplaintActionForm;
import com.hotel.booking.form.ComplaintFilter;
import com.hotel.booking.model.Complaint;
import com.hotel.booking.model.ComplaintPriority;
import com.hotel.booking.model.Role;
import com.hotel.booking.model.User;
import com.hotel.booking.service.BusinessException;
import com.hotel.booking.service.ComplaintService;
import com.hotel.booking.util.ListQuery;
import com.hotel.booking.util.PageResult;
import com.hotel.booking.util.Paginator;
import com.hotel.booking.util.Patterns;
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

/**
 * Complaint handling for administrators (US022: view, assign, track) and staff (US023: work on assigned complaints).
 * Both roles share the same screens; the URL prefix decides what the user may see.
 */
@Controller
public class ComplaintManagementController extends BaseController {

    private static final int PAGE_SIZE = 10;

    private final ComplaintService complaintService;

    public ComplaintManagementController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    private static final Map<String, Comparator<Complaint>> SORTABLE = new HashMap<>();

    static {
        SORTABLE.put("id", Comparator.comparing(Complaint::getReference));
        SORTABLE.put("customer", Comparator.comparing((Complaint c) -> c.getCustomer().getFullName().toLowerCase()));
        SORTABLE.put("date", Comparator.comparing(Complaint::getSubmittedAt));
        SORTABLE.put("category", Comparator.comparing((Complaint c) -> c.getCategory().ordinal()));
        SORTABLE.put("status", Comparator.comparing((Complaint c) -> c.getStatus().ordinal()));
        SORTABLE.put("priority", Comparator.comparing((Complaint c) -> c.getPriority().ordinal()));
        SORTABLE.put("staff", Comparator.comparing((Complaint c) -> c.getAssignedTo() == null ? "" : c.getAssignedTo().getFullName().toLowerCase()));
    }

    private static boolean isAdminPath(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/admin/");
    }

    private String base(HttpServletRequest request) {
        return isAdminPath(request) ? "/admin/complaints" : "/staff/complaints";
    }

    // ------------------------------------------------------------------ list

    @GetMapping({"/admin/complaints", "/staff/complaints"})
    public String list(@ModelAttribute("filter") ComplaintFilter filter, BindingResult errors,
                       @RequestParam(defaultValue = "1") int page, @RequestParam(required = false) String sort,
                       @RequestParam(required = false) String dir, @AuthenticationPrincipal AppUserDetails principal,
                       HttpServletRequest request, Model model) {
        boolean admin = isAdminPath(request);
        if (filter.getCustomer() != null && !filter.getCustomer().matches(Patterns.SEARCH_TEXT)) {
            errors.rejectValue("customer", "invalid", "Special characters are not allowed here (letters, digits, spaces, hyphens and apostrophes only).");
        }
        if (filter.getDateFrom() != null && filter.getDateTo() != null && filter.getDateTo().isBefore(filter.getDateFrom())) {
            errors.rejectValue("dateTo", "order", "The end date must not be before the start date.");
        }
        List<Complaint> found = errors.hasErrors() ? List.of() : complaintService.filter(filter, admin ? null : me(principal));
        PageResult<Complaint> result = Paginator.paginate(found, SORTABLE, sort == null ? "date" : sort, sort == null ? "desc" : dir, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("lq", new ListQuery(request));
        model.addAttribute("base", base(request));
        model.addAttribute("admin", admin);
        if (admin) model.addAttribute("staffList", userService.staff());
        return "complaint/manage-list";
    }

    // ------------------------------------------------------------------ details

    @GetMapping({"/admin/complaints/{id}", "/staff/complaints/{id}"})
    public String details(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal, HttpServletRequest request, Model model) {
        model.addAttribute("complaint", complaintService.getForStaff(id, me(principal)));
        model.addAttribute("actionForm", new ComplaintActionForm());
        return manageModel(request, model);
    }

    private String manageModel(HttpServletRequest request, Model model) {
        boolean admin = isAdminPath(request);
        model.addAttribute("base", base(request));
        model.addAttribute("admin", admin);
        if (admin) model.addAttribute("staffList", userService.staff());
        return "complaint/manage-details";
    }

    // ------------------------------------------------------------------ actions

    @PostMapping({"/admin/complaints/{id}/action", "/staff/complaints/{id}/action"})
    public String addAction(@PathVariable Long id, @Valid @ModelAttribute("actionForm") ComplaintActionForm form, BindingResult errors,
                            @AuthenticationPrincipal AppUserDetails principal, HttpServletRequest request, Model model,
                            RedirectAttributes ra) {
        User me = me(principal);
        if (!errors.hasErrors()) {
            try {
                complaintService.addAction(id, me, form);
                success(ra, "The complaint was updated and the customer has been notified where applicable.");
                return "redirect:" + base(request) + "/" + id;
            } catch (BusinessException e) {
                errors.reject("failed", e.getMessage());
            }
        }
        model.addAttribute("complaint", complaintService.getForStaff(id, me));
        return manageModel(request, model);
    }

    @PostMapping("/admin/complaints/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long staffId, @AuthenticationPrincipal AppUserDetails principal,
                         RedirectAttributes ra) {
        try {
            Complaint c = complaintService.assign(id, staffId, me(principal));
            success(ra, "Complaint " + c.getReference() + " was assigned to " + c.getAssignedTo().getFullName() + ", who has been notified.");
        } catch (BusinessException e) {
            error(ra, e.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }

    @PostMapping({"/admin/complaints/{id}/priority", "/staff/complaints/{id}/priority"})
    public String priority(@PathVariable Long id, @RequestParam ComplaintPriority priority, @AuthenticationPrincipal AppUserDetails principal,
                           HttpServletRequest request, RedirectAttributes ra) {
        try {
            complaintService.setPriority(id, priority, me(principal));
            success(ra, "Priority updated.");
        } catch (BusinessException e) {
            error(ra, e.getMessage());
        }
        return "redirect:" + base(request) + "/" + id;
    }
}

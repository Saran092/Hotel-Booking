package com.hotel.booking.api;

import com.hotel.booking.api.dto.ComplaintDto;
import com.hotel.booking.api.dto.UserSummary;
import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.form.ComplaintActionForm;
import com.hotel.booking.form.ComplaintFilter;
import com.hotel.booking.model.Complaint;
import com.hotel.booking.model.ComplaintPriority;
import com.hotel.booking.model.Role;
import com.hotel.booking.model.User;
import com.hotel.booking.security.RestExceptionHandler;
import com.hotel.booking.service.ComplaintService;
import com.hotel.booking.service.UserService;
import com.hotel.booking.util.Patterns;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Complaint handling for administrators (US022) and staff (US023).
 * Both roles share the same handlers; the URL prefix decides scoping and what the user may see.
 */
@RestController
public class ManageComplaintApi {

    private final ComplaintService complaintService;
    private final UserService userService;
    private final RestExceptionHandler forms;

    public ManageComplaintApi(ComplaintService complaintService, UserService userService, RestExceptionHandler forms) {
        this.complaintService = complaintService;
        this.userService = userService;
        this.forms = forms;
    }

    private static boolean isAdminPath(HttpServletRequest request) {
        return request.getRequestURI().contains("/api/admin/");
    }

    private User me(AppUserDetails principal) {
        return userService.get(principal.getId());
    }

    private static final Map<String, Comparator<Complaint>> SORTABLE = Map.of(
            "id", Comparator.comparing(Complaint::getReference),
            "customer", Comparator.comparing((Complaint c) -> c.getCustomer().getFullName().toLowerCase()),
            "date", Comparator.comparing(Complaint::getSubmittedAt),
            "category", Comparator.comparing((Complaint c) -> c.getCategory().ordinal()),
            "status", Comparator.comparing((Complaint c) -> c.getStatus().ordinal()),
            "priority", Comparator.comparing((Complaint c) -> c.getPriority().ordinal()),
            "staff", Comparator.comparing((Complaint c) -> c.getAssignedTo() == null ? "" : c.getAssignedTo().getFullName().toLowerCase()));

    @GetMapping({"/api/staff/complaints", "/api/admin/complaints"})
    public Map<String, Object> list(@ModelAttribute ComplaintFilter filter,
                                    @RequestParam(defaultValue = "date") String sort,
                                    @RequestParam(defaultValue = "desc") String dir,
                                    @AuthenticationPrincipal AppUserDetails principal,
                                    HttpServletRequest request) {
        boolean admin = isAdminPath(request);
        User actor = me(principal);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(filter, "filter");
        if (filter.getCustomer() != null && !filter.getCustomer().matches(Patterns.SEARCH_TEXT)) {
            errors.rejectValue("customer", "invalid",
                    "Special characters are not allowed here (letters, digits, spaces, hyphens and apostrophes only).");
        }
        if (filter.getDateFrom() != null && filter.getDateTo() != null && filter.getDateTo().isBefore(filter.getDateFrom())) {
            errors.rejectValue("dateTo", "order", "The end date must not be before the start date.");
        }
        if (errors.hasErrors()) {
            throw new com.hotel.booking.security.ApiValidationException(RestExceptionHandler.fieldErrors(errors));
        }
        List<Complaint> found = complaintService.filter(filter, admin ? null : actor);
        Comparator<Complaint> cmp = SORTABLE.getOrDefault(sort, SORTABLE.get("date"));
        if ("desc".equalsIgnoreCase(dir)) cmp = cmp.reversed();
        found = found.stream().sorted(cmp).toList();
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("complaints", found.stream().map(ComplaintDto::of).toList());
        m.put("admin", admin);
        if (admin) m.put("staffList", userService.staff().stream().map(UserSummary::brief).toList());
        return m;
    }

    @GetMapping({"/api/staff/complaints/{id}", "/api/admin/complaints/{id}"})
    public Map<String, Object> details(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal,
                                       HttpServletRequest request) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("complaint", ComplaintDto.of(complaintService.getForStaff(id, me(principal))));
        m.put("admin", isAdminPath(request));
        if (isAdminPath(request)) m.put("staffList", userService.staff().stream().map(UserSummary::brief).toList());
        return m;
    }

    @PostMapping({"/api/staff/complaints/{id}/action", "/api/admin/complaints/{id}/action"})
    @Transactional
    public ComplaintDto action(@PathVariable Long id, @RequestBody ComplaintActionForm form,
                               @AuthenticationPrincipal AppUserDetails principal) {
        var result = forms.validate(form);
        RestExceptionHandler.throwIfInvalid(result);
        return ComplaintDto.of(complaintService.addAction(id, me(principal), form));
    }

    @PostMapping("/api/admin/complaints/{id}/assign")
    @Transactional
    public Map<String, Object> assign(@PathVariable Long id, @RequestParam Long staffId,
                                      @AuthenticationPrincipal AppUserDetails principal) {
        Complaint c = complaintService.assign(id, staffId, me(principal));
        return Map.of("message", "Complaint " + c.getReference() + " was assigned to "
                + c.getAssignedTo().getFullName() + ", who has been notified.",
                "complaint", ComplaintDto.of(c));
    }

    @PostMapping({"/api/staff/complaints/{id}/priority", "/api/admin/complaints/{id}/priority"})
    @Transactional
    public Map<String, Object> priority(@PathVariable Long id, @RequestParam ComplaintPriority priority,
                                        @AuthenticationPrincipal AppUserDetails principal) {
        Complaint c = complaintService.setPriority(id, priority, me(principal));
        return Map.of("message", "Priority updated.", "complaint", ComplaintDto.of(c));
    }
}

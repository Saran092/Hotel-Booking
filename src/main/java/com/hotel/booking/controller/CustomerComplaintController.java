package com.hotel.booking.controller;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.form.ComplaintForm;
import com.hotel.booking.model.Complaint;
import com.hotel.booking.model.User;
import com.hotel.booking.service.BookingService;
import com.hotel.booking.service.ComplaintService;
import com.hotel.booking.util.ValidationGroups;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Customer side of complaints: register (US011) and track (US012). */
@Controller
@RequestMapping("/customer/complaints")
public class CustomerComplaintController extends BaseController {

    private final ComplaintService complaintService;
    private final BookingService bookingService;

    public CustomerComplaintController(ComplaintService complaintService, BookingService bookingService) {
        this.complaintService = complaintService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        model.addAttribute("complaints", complaintService.forCustomer(me(principal)));
        return "complaint/customer-list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        model.addAttribute("form", new ComplaintForm());
        model.addAttribute("myBookings", bookingService.history(me(principal)));
        model.addAttribute("editing", false);
        return "complaint/customer-form";
    }

    @PostMapping("/new")
    public String create(@AuthenticationPrincipal AppUserDetails principal,
                         @Validated(ValidationGroups.Ordered.class) @ModelAttribute("form") ComplaintForm form,
                         BindingResult errors, Model model, RedirectAttributes ra) {
        User u = me(principal);
        complaintService.validateBooking(form, u, errors);
        if (errors.hasErrors()) {
            model.addAttribute("myBookings", bookingService.history(u));
            model.addAttribute("editing", false);
            return "complaint/customer-form";
        }
        Complaint c = complaintService.submit(u, form);
        success(ra, "Your complaint has been successfully submitted. Complaint ID: #" + c.getReference()
                + ". Our support team will get back to you soon. A confirmation with the expected resolution date has been sent to you.");
        return "redirect:/customer/complaints/" + c.getId();
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal, Model model) {
        model.addAttribute("complaint", complaintService.getForCustomer(id, me(principal)));
        return "complaint/customer-details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal, Model model, RedirectAttributes ra) {
        User u = me(principal);
        Complaint c = complaintService.getForCustomer(id, u);
        if (c.getStatus() != com.hotel.booking.model.ComplaintStatus.OPEN) {
            error(ra, "Only complaints in the 'Open' status can be edited.");
            return "redirect:/customer/complaints/" + id;
        }
        model.addAttribute("form", complaintService.toForm(c));
        model.addAttribute("complaint", c);
        model.addAttribute("myBookings", bookingService.history(u));
        model.addAttribute("editing", true);
        return "complaint/customer-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal,
                       @Validated(ValidationGroups.Ordered.class) @ModelAttribute("form") ComplaintForm form,
                       BindingResult errors, Model model, RedirectAttributes ra) {
        User u = me(principal);
        complaintService.validateBooking(form, u, errors);
        if (errors.hasErrors()) {
            model.addAttribute("complaint", complaintService.getForCustomer(id, u));
            model.addAttribute("myBookings", bookingService.history(u));
            model.addAttribute("editing", true);
            return "complaint/customer-form";
        }
        complaintService.updateByCustomer(id, u, form);
        success(ra, "Your complaint has been updated.");
        return "redirect:/customer/complaints/" + id;
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal, RedirectAttributes ra) {
        complaintService.confirmResolution(id, me(principal));
        success(ra, "Thank you! The complaint has been closed.");
        return "redirect:/customer/complaints/" + id;
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal, RedirectAttributes ra) {
        complaintService.reopen(id, me(principal));
        success(ra, "The complaint has been reopened. Our support team will look into it again.");
        return "redirect:/customer/complaints/" + id;
    }
}

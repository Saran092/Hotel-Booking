package com.hotel.booking.api;

import com.hotel.booking.api.dto.BookingDto;
import com.hotel.booking.api.dto.ComplaintDto;
import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.form.ComplaintForm;
import com.hotel.booking.model.Booking;
import com.hotel.booking.model.Complaint;
import com.hotel.booking.model.ComplaintStatus;
import com.hotel.booking.model.User;
import com.hotel.booking.service.BookingService;
import com.hotel.booking.service.ComplaintService;
import com.hotel.booking.service.UserService;
import com.hotel.booking.security.RestExceptionHandler;
import com.hotel.booking.util.ValidationGroups;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Customer side of complaints: register (US011) and track (US012). */
@RestController
@RequestMapping("/api/complaints")
public class CustomerComplaintApi {

    private final ComplaintService complaintService;
    private final BookingService bookingService;
    private final UserService userService;
    private final RestExceptionHandler forms;

    public CustomerComplaintApi(ComplaintService complaintService, BookingService bookingService,
                                UserService userService, RestExceptionHandler forms) {
        this.complaintService = complaintService;
        this.bookingService = bookingService;
        this.userService = userService;
        this.forms = forms;
    }

    @GetMapping
    public List<ComplaintDto> list(@AuthenticationPrincipal AppUserDetails principal) {
        return complaintService.forCustomer(userService.get(principal.getId())).stream()
                .map(c -> ComplaintDto.of(c, true)).toList();
    }

    /** Booking choices + enum labels for the complaint form. */
    @GetMapping("/form-options")
    public Map<String, Object> formOptions(@AuthenticationPrincipal AppUserDetails principal) {
        List<Booking> bookings = bookingService.history(userService.get(principal.getId()));
        return Map.of("myBookings", bookings.stream()
                .sorted(java.util.Comparator.comparing(Booking::getCheckInDate).reversed())
                .map(b -> Map.of("id", b.getId(), "bookingRef", b.getBookingRef(),
                        "label", b.getBookingRef() + " — " + b.getRoom().getRoomNumber() + " (" + b.getRoom().getRoomType().getLabel() + ")"))
                .toList());
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody ComplaintForm form,
                                      @AuthenticationPrincipal AppUserDetails principal) {
        User u = userService.get(principal.getId());
        var result = forms.validate(form, ValidationGroups.Ordered.class);
        complaintService.validateBooking(form, u, result);
        RestExceptionHandler.throwIfInvalid(result);
        Complaint c = complaintService.submit(u, form);
        return Map.of("id", c.getId(), "reference", c.getReference(),
                "message", "Your complaint has been successfully submitted. Complaint ID: #" + c.getReference()
                        + ". Our support team will get back to you soon. A confirmation with the expected resolution date has been sent to you.");
    }

    @GetMapping("/{id}")
    public ComplaintDto details(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return ComplaintDto.of(complaintService.getForCustomer(id, userService.get(principal.getId())), true);
    }

    @GetMapping("/{id}/edit")
    public Map<String, Object> editForm(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        User u = userService.get(principal.getId());
        Complaint c = complaintService.getForCustomer(id, u);
        if (c.getStatus() != ComplaintStatus.OPEN) {
            throw new com.hotel.booking.service.BusinessException("Only complaints in the 'Open' status can be edited.");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("form", complaintService.toForm(c));
        m.put("myBookings", bookingService.history(u).stream()
                .sorted(java.util.Comparator.comparing(Booking::getCheckInDate).reversed())
                .map(b -> Map.of("id", b.getId(), "bookingRef", b.getBookingRef()))
                .toList());
        return m;
    }

    @PutMapping("/{id}/edit")
    @Transactional
    public Map<String, Object> edit(@PathVariable Long id, @RequestBody ComplaintForm form,
                                    @AuthenticationPrincipal AppUserDetails principal) {
        User u = userService.get(principal.getId());
        var result = forms.validate(form, ValidationGroups.Ordered.class);
        complaintService.validateBooking(form, u, result);
        RestExceptionHandler.throwIfInvalid(result);
        complaintService.updateByCustomer(id, u, form);
        return Map.of("message", "Your complaint has been updated.");
    }

    @PostMapping("/{id}/confirm")
    @Transactional
    public Map<String, Object> confirm(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Complaint c = complaintService.confirmResolution(id, userService.get(principal.getId()));
        return Map.of("message", "Thank you! The complaint has been closed.", "complaint", ComplaintDto.of(c, true));
    }

    @PostMapping("/{id}/reopen")
    @Transactional
    public Map<String, Object> reopen(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Complaint c = complaintService.reopen(id, userService.get(principal.getId()));
        return Map.of("message", "The complaint has been reopened. Our support team will look into it again.",
                "complaint", ComplaintDto.of(c, true));
    }
}

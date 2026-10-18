package com.hotel.booking.controller;

import com.hotel.booking.form.AdminReservationForm;
import com.hotel.booking.form.ReservationFilter;
import com.hotel.booking.model.Booking;
import com.hotel.booking.model.BookingStatus;
import com.hotel.booking.service.BookingService;
import com.hotel.booking.service.BusinessException;
import com.hotel.booking.service.RoomService;
import com.hotel.booking.util.ListQuery;
import com.hotel.booking.util.PageResult;
import com.hotel.booking.util.Paginator;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.repository.RoomRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/** US017 (view / add / modify / cancel reservations) and US018 (search reservations). */
@Controller
@RequestMapping("/admin/reservations")
public class AdminReservationController extends BaseController {

    private static final int PAGE_SIZE = 10;
    private static final String BAD_TEXT = "Special characters are not allowed here (letters, digits, spaces, hyphens and apostrophes only).";

    private final BookingService bookingService;
    private final RoomRepository rooms;

    public AdminReservationController(BookingService bookingService, RoomRepository rooms) {
        this.bookingService = bookingService;
        this.rooms = rooms;
    }

    private static final Map<String, Comparator<Booking>> SORTABLE = new HashMap<>();

    static {
        SORTABLE.put("id", Comparator.comparing(Booking::getBookingRef));
        SORTABLE.put("customer", Comparator.comparing((Booking b) -> b.getCustomer().getFullName().toLowerCase()));
        SORTABLE.put("room", Comparator.comparing((Booking b) -> b.getRoom().getRoomNumber()));
        SORTABLE.put("checkIn", Comparator.comparing(Booking::getCheckInDate));
        SORTABLE.put("checkOut", Comparator.comparing(Booking::getCheckOutDate));
        SORTABLE.put("status", Comparator.comparing((Booking b) -> b.getEffectiveStatus().ordinal()));
        SORTABLE.put("total", Comparator.comparing(Booking::getTotalAmount));
        SORTABLE.put("booked", Comparator.comparing(Booking::getCreatedAt));
    }

    private void checkText(BindingResult errors, String field, String value) {
        if (value != null && !value.matches(Patterns.SEARCH_TEXT)) errors.rejectValue(field, "invalid", BAD_TEXT);
    }

    @GetMapping
    public String list(@ModelAttribute("filter") ReservationFilter filter, BindingResult errors,
                       @RequestParam(defaultValue = "1") int page, @RequestParam(required = false) String sort,
                       @RequestParam(required = false) String dir, HttpServletRequest request, Model model) {
        checkText(errors, "reservationId", filter.getReservationId());
        checkText(errors, "customer", filter.getCustomer());
        checkText(errors, "roomNumber", filter.getRoomNumber());
        if (filter.getCheckIn() != null && filter.getCheckOut() != null && filter.getCheckOut().isBefore(filter.getCheckIn())) {
            errors.rejectValue("checkOut", "order", "The check-out date must not be before the check-in date.");
        }
        List<Booking> found = errors.hasErrors() ? List.of() : bookingService.filter(filter);
        PageResult<Booking> result = Paginator.paginate(found, SORTABLE, sort == null ? "booked" : sort, sort == null ? "desc" : dir,
                page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("lq", new ListQuery(request));
        return "admin/reservations";
    }

    private void formModel(Model model) {
        model.addAttribute("allRooms", rooms.findAll().stream().sorted(Comparator.comparing(r -> r.getRoomNumber())).toList());
        model.addAttribute("editableStatuses", List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.CHECKED_OUT));
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new AdminReservationForm());
        formModel(model);
        return "admin/reservation-form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("form") AdminReservationForm form, BindingResult errors, Model model, RedirectAttributes ra) {
        bookingService.validateAdminReservation(form, null, errors);
        if (errors.hasErrors()) {
            formModel(model);
            return "admin/reservation-form";
        }
        Booking b = bookingService.adminCreate(form);
        success(ra, "Reservation " + b.getBookingRef() + " was created for " + b.getCustomer().getFullName() + ".");
        return "redirect:/admin/reservations";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Booking b = bookingService.get(id);
        model.addAttribute("booking", b);
        model.addAttribute("form", bookingService.toAdminForm(b));
        formModel(model);
        return "admin/reservation-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute("form") AdminReservationForm form, BindingResult errors,
                       Model model, RedirectAttributes ra) {
        Booking b = bookingService.get(id);
        bookingService.validateAdminReservation(form, b, errors);
        if (!errors.hasErrors()) {
            try {
                bookingService.adminUpdate(b, form);
                success(ra, "Reservation " + b.getBookingRef() + " was updated.");
                return "redirect:/admin/reservations";
            } catch (BusinessException e) {
                errors.reject("failed", e.getMessage());
            }
        }
        model.addAttribute("booking", b);
        formModel(model);
        return "admin/reservation-form";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Booking b = bookingService.cancel(bookingService.get(id), true);
            success(ra, "Reservation " + b.getBookingRef() + " was canceled."
                    + (b.getRefundAmount().signum() > 0 ? " A refund of " + BookingService.money(b.getRefundAmount()) + " will be processed." : ""));
        } catch (BusinessException e) {
            error(ra, e.getMessage());
        }
        return "redirect:/admin/reservations";
    }
}

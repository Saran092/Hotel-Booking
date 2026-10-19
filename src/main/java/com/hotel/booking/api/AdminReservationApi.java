package com.hotel.booking.api;

import com.hotel.booking.api.dto.BookingDto;
import com.hotel.booking.api.dto.RoomDto;
import com.hotel.booking.form.AdminReservationForm;
import com.hotel.booking.form.ReservationFilter;
import com.hotel.booking.model.Booking;
import com.hotel.booking.model.BookingStatus;
import com.hotel.booking.model.Room;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.security.RestExceptionHandler;
import com.hotel.booking.service.BookingService;
import com.hotel.booking.service.BusinessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** US017 (view / add / modify / cancel reservations) and US018 (search reservations). */
@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationApi {

    private final BookingService bookingService;
    private final RoomRepository rooms;
    private final RestExceptionHandler forms;

    public AdminReservationApi(BookingService bookingService, RoomRepository rooms, RestExceptionHandler forms) {
        this.bookingService = bookingService;
        this.rooms = rooms;
        this.forms = forms;
    }

    @GetMapping
    public Map<String, Object> list(@ModelAttribute ReservationFilter filter) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(filter, "filter");
        if (filter.getReservationId() != null && !filter.getReservationId().matches(com.hotel.booking.util.Patterns.SEARCH_TEXT)) {
            errors.rejectValue("reservationId", "invalid", "Special characters are not allowed here (letters, digits, spaces, hyphens and apostrophes only).");
        }
        if (filter.getCustomer() != null && !filter.getCustomer().matches(com.hotel.booking.util.Patterns.SEARCH_TEXT)) {
            errors.rejectValue("customer", "invalid", "Special characters are not allowed here (letters, digits, spaces, hyphens and apostrophes only).");
        }
        if (filter.getRoomNumber() != null && !filter.getRoomNumber().matches(com.hotel.booking.util.Patterns.SEARCH_TEXT)) {
            errors.rejectValue("roomNumber", "invalid", "Special characters are not allowed here (letters, digits, spaces, hyphens and apostrophes only).");
        }
        if (filter.getCheckIn() != null && filter.getCheckOut() != null && filter.getCheckOut().isBefore(filter.getCheckIn())) {
            errors.rejectValue("checkOut", "order", "The check-out date must not be before the check-in date.");
        }
        if (errors.hasErrors()) {
            throw new com.hotel.booking.security.ApiValidationException(RestExceptionHandler.fieldErrors(errors));
        }
        List<Booking> found = bookingService.filter(filter);
        return Map.of("reservations", found.stream().map(BookingDto::of).toList());
    }

    private Map<String, Object> formModel() {
        List<Room> allRooms = rooms.findAll().stream().sorted(Comparator.comparing(Room::getRoomNumber)).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("allRooms", allRooms.stream().map(RoomDto::of).toList());
        m.put("editableStatuses", List.of(BookingStatus.CONFIRMED.name(), BookingStatus.CHECKED_IN.name(), BookingStatus.CHECKED_OUT.name()));
        return m;
    }

    @GetMapping("/new")
    public Map<String, Object> newForm() {
        Map<String, Object> m = new LinkedHashMap<>(formModel());
        m.put("form", new AdminReservationForm());
        return m;
    }

    @PostMapping
    @Transactional
    public BookingDto create(@RequestBody AdminReservationForm form) {
        var result = forms.validate(form);
        bookingService.validateAdminReservation(form, null, result);
        RestExceptionHandler.throwIfInvalid(result);
        return BookingDto.of(bookingService.adminCreate(form));
    }

    @GetMapping("/{id}/edit")
    public Map<String, Object> editForm(@PathVariable Long id) {
        Booking b = bookingService.get(id);
        Map<String, Object> m = new LinkedHashMap<>(formModel());
        m.put("booking", BookingDto.of(b));
        m.put("form", bookingService.toAdminForm(b));
        return m;
    }

    @PutMapping("/{id}/edit")
    @Transactional
    public Map<String, Object> edit(@PathVariable Long id, @RequestBody AdminReservationForm form) {
        Booking b = bookingService.get(id);
        var result = forms.validate(form);
        bookingService.validateAdminReservation(form, b, result);
        RestExceptionHandler.throwIfInvalid(result);
        bookingService.adminUpdate(b, form);
        return Map.of("message", "Reservation " + b.getBookingRef() + " was updated.", "booking", BookingDto.of(b));
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public Map<String, Object> cancel(@PathVariable Long id) {
        Booking b = bookingService.cancel(bookingService.get(id), true);
        String message = "Reservation " + b.getBookingRef() + " was canceled."
                + (b.getRefundAmount().signum() > 0 ? " A refund of " + BookingService.money(b.getRefundAmount()) + " will be processed." : "");
        return Map.of("message", message, "booking", BookingDto.of(b));
    }
}

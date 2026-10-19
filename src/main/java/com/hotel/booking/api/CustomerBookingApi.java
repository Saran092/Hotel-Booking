package com.hotel.booking.api;

import com.hotel.booking.api.dto.*;
import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.form.*;
import com.hotel.booking.model.*;
import com.hotel.booking.repository.PaymentRepository;
import com.hotel.booking.service.*;
import com.hotel.booking.security.RestExceptionHandler;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Customer journey: search, book, pay, invoice/receipt PDFs, history, details, modify and cancel. */
@RestController
@RequestMapping("/api")
public class CustomerBookingApi {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final HotelProperties props;
    private final PendingPaymentStore store;
    private final UserService userService;

    public CustomerBookingApi(RoomService roomService, BookingService bookingService, PaymentService paymentService,
                              PaymentRepository paymentRepository, InvoiceService invoiceService, HotelProperties props,
                              PendingPaymentStore store, UserService userService) {
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
        this.props = props;
        this.store = store;
        this.userService = userService;
    }

    // ================================================================== search

    @PostMapping("/rooms/search")
    public Map<String, Object> search(@RequestBody RoomSearchForm form) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        StayValidator.validate(form.getCheckIn(), form.getCheckOut(), form.getAdults(), form.getChildren(), errors, false);
        if (form.getRoomType() == null) errors.rejectValue("roomType", "required", "Please select a room type.");
        RestExceptionHandler.throwIfInvalid(errors);
        List<PriceQuoteDto> quotes = roomService.findAvailable(form).stream()
                .map(r -> PriceQuoteDto.of(bookingService.quote(r, form.getCheckIn(), form.getCheckOut())))
                .collect(Collectors.toList());
        return Map.of("quotes", quotes, "searched", true);
    }

    // ================================================================== booking (US005)

    /** Re-validates a booking request; returns an error message or null. */
    private String checkRequest(BookingRequestForm req) {
        if (req.getRoomId() == null || req.getCheckIn() == null || req.getCheckOut() == null || req.getAdults() == null) {
            return "Please search for a room first.";
        }
        BeanPropertyBindingResult tmp = new BeanPropertyBindingResult(req, "req");
        StayValidator.validate(req.getCheckIn(), req.getCheckOut(), req.getAdults(), req.getChildren(), tmp, false);
        if (tmp.hasErrors()) return tmp.getAllErrors().get(0).getDefaultMessage();
        Room room = roomService.get(req.getRoomId());
        int guests = req.getAdults() + (req.getChildren() == null ? 0 : req.getChildren());
        if (guests > room.getMaxOccupancy()) {
            return "Number of adults and children must not exceed the room occupancy limit (max " + room.getMaxOccupancy() + ").";
        }
        if (!roomService.isAvailable(room, req.getCheckIn(), req.getCheckOut(), null)) {
            return "Sorry, this room is no longer available for the selected dates. Please search again.";
        }
        return null;
    }

    /** Booking confirmation preview ("Book Now" on a search result). */
    @PostMapping("/bookings/preview")
    public PriceQuoteDto preview(@RequestBody BookingRequestForm req) {
        String problem = checkRequest(req);
        if (problem != null) throw new BusinessException(problem);
        return PriceQuoteDto.of(bookingService.quote(roomService.get(req.getRoomId()), req.getCheckIn(), req.getCheckOut()));
    }

    @PostMapping("/bookings")
    @Transactional
    public Map<String, Object> create(@RequestBody BookingRequestForm req,
                                      @AuthenticationPrincipal AppUserDetails principal) {
        String problem = checkRequest(req);
        if (problem != null) throw new BusinessException(problem);
        if (req.getPaymentMethod() == null) throw new BusinessException("Please select a payment method.");
        User customer = currentUser(principal);
        Booking booking = bookingService.createPending(customer, req);
        store.putPayment(customer.getId(), booking.getId(),
                new PaymentContext(PaymentContext.BOOKING, booking.getId(), booking.getTotalAmount(), null));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bookingId", booking.getId());
        m.put("bookingRef", booking.getBookingRef());
        m.put("amount", booking.getTotalAmount());
        m.put("holdExpiresAt", booking.getHoldExpiresAt());
        m.put("paymentSessionMinutes", props.getPaymentSessionMinutes());
        return m;
    }

    // ================================================================== payment (US006)

    @GetMapping("/bookings/{id}/payment")
    public Map<String, Object> paymentForm(@PathVariable Long id,
                                           @AuthenticationPrincipal AppUserDetails principal) {
        User customer = currentUser(principal);
        PaymentContext ctx = store.getPayment(customer.getId(), id);
        if (ctx == null) throw new BusinessException("There is no payment in progress.");
        Booking booking = bookingService.getForCustomer(id, customer);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("booking", BookingDto.of(booking));
        m.put("amount", ctx.getAmount());
        m.put("type", ctx.getType());
        m.put("paymentSessionMinutes", props.getPaymentSessionMinutes());
        return m;
    }

    @PostMapping("/bookings/{id}/payment")
    @Transactional
    public Map<String, Object> pay(@PathVariable Long id, @RequestBody PaymentForm form,
                                   @AuthenticationPrincipal AppUserDetails principal) {
        User customer = currentUser(principal);
        PaymentContext ctx = store.getPayment(customer.getId(), id);
        if (ctx == null) throw new BusinessException("There is no payment in progress.");
        Booking booking = bookingService.getForCustomer(id, customer);
        PaymentMethod method = booking.getPaymentMethod();

        if (ctx.isExpired(props.getPaymentSessionMinutes())) {
            ctx.touch();
            if (!ctx.isModification()) bookingService.refreshHold(booking);
            return result("EXPIRED", "Your session has expired. Please re-enter your payment details.");
        }
        if (!ctx.isModification() && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("This booking is not waiting for payment.");
        }

        Map<String, String> fieldErrors = paymentService.validate(method, form);
        if (!fieldErrors.isEmpty()) {
            Map<String, Object> m = result("INVALID", "Please correct the errors below.");
            m.put("errors", fieldErrors);
            return m;
        }

        Payment payment = paymentService.charge(booking, ctx.getAmount(), method, form, ctx.getType());
        if (!payment.isSuccess()) {
            return result("DECLINED", "Transaction failed. Please check your details and try again.");
        }

        store.clearPayment(customer.getId(), id);
        if (ctx.isModification()) {
            store.clearDraft(customer.getId(), id);
            bookingService.applyModification(booking, ctx.getDraft(), payment);
            return result("SUCCESS", "Payment successful! Your booking has been modified. You can download the updated invoice below.");
        }
        bookingService.markPaid(booking, payment);
        return result("SUCCESS", "Payment successful! Your booking is confirmed.");
    }

    @GetMapping("/bookings/{id}/success")
    public Map<String, Object> success(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Booking booking = bookingService.getForCustomer(id, currentUser(principal));
        Payment payment = paymentRepository.findFirstByBookingAndSuccessTrueOrderByCreatedAtDesc(booking).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("booking", BookingDto.of(booking));
        m.put("payment", PaymentDto.of(payment));
        return m;
    }

    // ================================================================== PDFs (US007)

    @GetMapping("/bookings/{id}/receipt.pdf")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Booking booking = bookingService.getForCustomer(id, currentUser(principal));
        if (booking.getAmountPaid().signum() <= 0) throw new BusinessException("A receipt is available only after payment.");
        return pdf(invoiceService.receipt(booking), "receipt-" + booking.getBookingRef() + ".pdf");
    }

    @GetMapping("/bookings/{id}/invoice.pdf")
    public ResponseEntity<byte[]> invoice(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Booking booking = bookingService.getForCustomer(id, currentUser(principal));
        if (booking.getPaymentStatus() == PaymentStatus.PARTIALLY_PAID || booking.getPaymentStatus() == PaymentStatus.PENDING) {
            throw new BusinessException("Invoice will be available after full payment.");
        }
        if (!booking.isPaid()) throw new BusinessException("An invoice can only be generated for paid bookings that are not canceled.");
        return pdf(invoiceService.invoice(booking), "invoice-" + booking.getBookingRef() + ".pdf");
    }

    private ResponseEntity<byte[]> pdf(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setCacheControl("no-store");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    // ================================================================== history + details (US008)

        @GetMapping("/bookings")
    public java.util.Map<String, Object> history(@AuthenticationPrincipal AppUserDetails principal) {
        java.util.List<Booking> all = bookingService.history(userService.get(principal.getId()));
        java.util.List<com.hotel.booking.api.dto.BookingDto> upcoming = all.stream().filter(Booking::isUpcoming)
                .sorted(java.util.Comparator.comparing(Booking::getCheckInDate))
                .map(com.hotel.booking.api.dto.BookingDto::of)
                .collect(java.util.stream.Collectors.toList());
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("upcoming", upcoming);
        m.put("upcomingCount", Integer.valueOf(upcoming.size()));
        m.put("past", all.stream().filter(b -> !b.isUpcoming())
                .sorted(java.util.Comparator.comparing(Booking::getCheckInDate).reversed())
                .map(com.hotel.booking.api.dto.BookingDto::of)
                .collect(java.util.stream.Collectors.toList()));
        return m;
    }
@GetMapping("/bookings/{id}")
    public Map<String, Object> details(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, currentUser(principal));
        boolean modifiable = true;
        try {
            bookingService.checkModifiable(b);
        } catch (BusinessException e) {
            modifiable = false;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("booking", BookingDto.of(b));
        m.put("cancellation", CancellationQuoteDto.of(bookingService.cancellationQuote(b)));
        m.put("modifiable", modifiable);
        return m;
    }

    // ================================================================== cancel (US010)

    @GetMapping("/bookings/{id}/cancel")
    public Map<String, Object> cancelQuote(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, currentUser(principal));
        CancellationQuote q = bookingService.cancellationQuote(b);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("booking", BookingDto.of(b));
        m.put("quote", CancellationQuoteDto.of(q));
        return m;
    }

    @PostMapping("/bookings/{id}/cancel")
    @Transactional
    public Map<String, Object> cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, currentUser(principal));
        try {
            bookingService.cancel(b, false);
        } catch (BusinessException e) {
            throw e;
        }
        String message = b.getRefundAmount().signum() > 0
                ? "Your booking has been canceled. A refund of " + BookingService.money(b.getRefundAmount())
                        + " will be processed within 3-5 business days."
                : "Your booking has been canceled. As per the hotel's policy, this booking is non-refundable.";
        return Map.of("message", message, "booking", BookingDto.of(b));
    }

    // ================================================================== modify (US009)

    @GetMapping("/bookings/{id}/modify")
    public Map<String, Object> modifyForm(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, currentUser(principal));
        bookingService.checkModifiable(b);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("booking", BookingDto.of(b));
        m.put("form", bookingService.toModifyForm(b));
        m.put("today", LocalDate.now());
        return m;
    }

    @PostMapping("/bookings/{id}/modify/quote")
    public Map<String, Object> modifyQuote(@PathVariable Long id, @RequestBody ModifyBookingForm form,
                                           @AuthenticationPrincipal AppUserDetails principal) {
        User customer = currentUser(principal);
        Booking b = bookingService.getForCustomer(id, customer);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        ModificationDraft draft;
        try {
            draft = bookingService.prepareModification(b, form, errors);
        } catch (BusinessException e) {
            throw e;
        }
        if (draft == null) throw new com.hotel.booking.security.ApiValidationException(RestExceptionHandler.fieldErrors(errors));
        store.putDraft(customer.getId(), id, draft);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("message", "Room " + draft.getRoomNumber() + " (" + draft.getRoomType().getLabel() + ") is available. New total: "
                + BookingService.money(draft.getTotal()) + (draft.getDifference().signum() == 0 ? "" : draft.getDifference().signum() > 0
                ? " (additional " + BookingService.money(draft.getDifference()) + " to pay)"
                : " (refund " + BookingService.money(draft.getDifference().negate()) + ")"));
        m.put("draft", draftDto(draft));
        return m;
    }

    @PostMapping("/bookings/{id}/modify/confirm")
    @Transactional
    public Map<String, Object> modifyConfirm(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        User customer = currentUser(principal);
        Booking b = bookingService.getForCustomer(id, customer);
        ModificationDraft draft = store.getDraft(customer.getId(), id);
        if (draft == null) throw new BusinessException("Please choose the changes you want to make first.");
        if (draft.getDifference().signum() > 0) {
            store.putPayment(customer.getId(), id,
                    new PaymentContext(PaymentContext.MODIFICATION, b.getId(), draft.getDifference(), draft));
            return Map.of("status", "PAYMENT_REQUIRED", "amount", draft.getDifference());
        }
        bookingService.applyModification(b, draft, null);
        store.clearDraft(customer.getId(), id);
        String message = draft.getDifference().signum() < 0
                ? "Your booking was updated. Your updated booking costs less. Refund of "
                        + BookingService.money(draft.getDifference().negate()) + " will be processed within 3-5 business days."
                : "Your booking was updated successfully.";
        return Map.of("status", "SUCCESS", "message", message);
    }

    /** Live availability check used by the modify form while the customer changes dates or room type. */
    @GetMapping("/bookings/{id}/modify/availability")
    public Map<String, Object> modifyAvailability(@PathVariable Long id, @ModelAttribute ModifyBookingForm form,
                                                  @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, currentUser(principal));
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(form, "form");
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            ModificationDraft d = bookingService.prepareModification(b, form, br);
            if (d == null) {
                result.put("ok", false);
                result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            } else {
                result.put("ok", true);
                result.put("message", "Room " + d.getRoomNumber() + " (" + d.getRoomType().getLabel() + ") is available. New total: "
                        + BookingService.money(d.getTotal()) + (d.getDifference().signum() == 0 ? "" : d.getDifference().signum() > 0
                        ? " (additional " + BookingService.money(d.getDifference()) + " to pay)"
                        : " (refund " + BookingService.money(d.getDifference().negate()) + ")"));
            }
        } catch (BusinessException e) {
            result.put("ok", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ================================================================== helpers

    private User currentUser(AppUserDetails principal) {
        return userService.get(principal.getId());
    }

    private static Map<String, Object> result(String status, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("message", message);
        return m;
    }

    private static Map<String, Object> draftDto(ModificationDraft d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bookingId", d.getBookingId());
        m.put("roomId", d.getRoomId());
        m.put("roomNumber", d.getRoomNumber());
        m.put("roomType", d.getRoomType().name());
        m.put("checkIn", d.getCheckIn());
        m.put("checkOut", d.getCheckOut());
        m.put("adults", d.getAdults());
        m.put("children", d.getChildren());
        m.put("nights", d.getNights());
        m.put("nightlyRate", d.getNightlyRate());
        m.put("base", d.getBase());
        m.put("gst", d.getGst());
        m.put("service", d.getService());
        m.put("total", d.getTotal());
        m.put("difference", d.getDifference());
        return m;
    }
}

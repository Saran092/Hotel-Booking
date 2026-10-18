package com.hotel.booking.controller;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.form.*;
import com.hotel.booking.model.*;
import com.hotel.booking.repository.PaymentRepository;
import com.hotel.booking.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Customer booking journey: search (US004), book + confirm (US005), pay (US006), invoice (US007),
 * history (US008), modify (US009) and cancel (US010).
 */
@Controller
@RequestMapping("/customer")
public class CustomerBookingController extends BaseController {

    private static final String PAYMENT_CTX = "paymentContext";
    private static final String MOD_DRAFT = "modificationDraft";

    private final RoomService roomService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final HotelProperties props;

    public CustomerBookingController(RoomService roomService, BookingService bookingService, PaymentService paymentService,
                                     PaymentRepository paymentRepository, InvoiceService invoiceService, HotelProperties props) {
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
        this.props = props;
    }

    // =================================================================== US004 search

    @GetMapping("/rooms/search")
    public String search(@ModelAttribute("form") RoomSearchForm form, BindingResult errors,
                         @RequestParam(required = false) String search, Model model) {
        if (search != null) {
            StayValidator.validate(form.getCheckIn(), form.getCheckOut(), form.getAdults(), form.getChildren(), errors, false);
            if (form.getRoomType() == null) errors.rejectValue("roomType", "required", "Please select a room type.");
            if (!errors.hasErrors()) {
                List<PriceQuote> quotes = roomService.findAvailable(form).stream()
                        .map(r -> bookingService.quote(r, form.getCheckIn(), form.getCheckOut()))
                        .collect(Collectors.toList());
                model.addAttribute("quotes", quotes);
                model.addAttribute("searched", true);
            }
        }
        model.addAttribute("today", java.time.LocalDate.now());
        return "booking/search";
    }

    // =================================================================== US005 booking confirmation

    /** "Book Now" on a search result: shows the pre-filled booking confirmation page. */
    @PostMapping("/booking/confirm")
    public String confirm(@ModelAttribute("req") BookingRequestForm req, BindingResult errors, Model model, RedirectAttributes ra) {
        String problem = checkRequest(req);
        if (problem != null) {
            error(ra, problem);
            return "redirect:/customer/rooms/search";
        }
        return confirmationPage(req, model);
    }

    /** Re-validates a booking request that comes from the browser. Returns an error message or null. */
    private String checkRequest(BookingRequestForm req) {
        if (req.getRoomId() == null || req.getCheckIn() == null || req.getCheckOut() == null || req.getAdults() == null) {
            return "Please search for a room first.";
        }
        BindingResult tmp = new BeanPropertyBindingResult(req, "req");
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

    private String confirmationPage(BookingRequestForm req, Model model) {
        Room room = roomService.get(req.getRoomId());
        model.addAttribute("quote", bookingService.quote(room, req.getCheckIn(), req.getCheckOut()));
        return "booking/confirm";
    }

    @PostMapping("/booking/pay")
    public String proceedToPay(@Valid @ModelAttribute("req") BookingRequestForm req, BindingResult errors, Model model,
                               HttpSession session, RedirectAttributes ra, @AuthenticationPrincipal AppUserDetails principal) {
        String problem = checkRequest(req);
        if (problem != null) {
            error(ra, problem);
            return "redirect:/customer/rooms/search";
        }
        if (req.getPaymentMethod() == null) {
            errors.rejectValue("paymentMethod", "required", "Please select a payment method.");
        }
        if (errors.hasErrors()) return confirmationPage(req, model);
        Booking booking = bookingService.createPending(me(principal), req);
        session.setAttribute(PAYMENT_CTX, new PaymentContext(PaymentContext.BOOKING, booking.getId(), booking.getTotalAmount(), null));
        return "redirect:/customer/payment";
    }

    // =================================================================== US006 payment

    private PaymentContext context(HttpSession session) {
        return (PaymentContext) session.getAttribute(PAYMENT_CTX);
    }

    private String paymentPage(Model model, PaymentContext ctx, Booking booking, PaymentMethod method, PaymentForm form) {
        model.addAttribute("ctx", ctx);
        model.addAttribute("booking", booking);
        model.addAttribute("method", method == null ? PaymentMethod.CREDIT_CARD : method);
        form.setCvv("");
        model.addAttribute("form", form);
        model.addAttribute("minutes", props.getPaymentSessionMinutes());
        return "booking/payment";
    }

    @GetMapping("/payment")
    public String paymentForm(HttpSession session, Model model, RedirectAttributes ra, @AuthenticationPrincipal AppUserDetails principal) {
        PaymentContext ctx = context(session);
        if (ctx == null) {
            error(ra, "There is no payment in progress.");
            return "redirect:/customer/bookings";
        }
        Booking booking = bookingService.getForCustomer(ctx.getBookingId(), me(principal));
        return paymentPage(model, ctx, booking, booking.getPaymentMethod(), new PaymentForm());
    }

    @PostMapping("/payment")
    public String pay(@RequestParam(required = false) PaymentMethod method, @ModelAttribute("form") PaymentForm form,
                      HttpSession session, Model model, RedirectAttributes ra, @AuthenticationPrincipal AppUserDetails principal) {
        PaymentContext ctx = context(session);
        if (ctx == null) {
            error(ra, "There is no payment in progress.");
            return "redirect:/customer/bookings";
        }
        Booking booking = bookingService.getForCustomer(ctx.getBookingId(), me(principal));

        if (ctx.isExpired(props.getPaymentSessionMinutes())) {
            ctx.touch();
            if (!ctx.isModification()) bookingService.refreshHold(booking);
            ra.addFlashAttribute("warning", "Your session has expired. Please re-enter your payment details.");
            return "redirect:/customer/payment";
        }
        if (!ctx.isModification() && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            error(ra, "This booking is not waiting for payment.");
            return "redirect:/customer/bookings/" + booking.getId();
        }

        Map<String, String> fieldErrors = paymentService.validate(method, form);
        if (!fieldErrors.isEmpty()) {
            model.addAttribute("fieldErrors", fieldErrors);
            return paymentPage(model, ctx, booking, method, form);
        }

        Payment payment = paymentService.charge(booking, ctx.getAmount(), method, form, ctx.getType());
        if (!payment.isSuccess()) {
            model.addAttribute("paymentError", "Transaction failed. Please check your details and try again.");
            return paymentPage(model, ctx, booking, method, form);
        }

        session.removeAttribute(PAYMENT_CTX);
        if (ctx.isModification()) {
            session.removeAttribute(MOD_DRAFT);
            bookingService.applyModification(booking, ctx.getDraft(), payment);
            success(ra, "Payment successful! Your booking has been modified. You can download the updated invoice below.");
            return "redirect:/customer/bookings/" + booking.getId();
        }
        bookingService.markPaid(booking, payment);
        return "redirect:/customer/booking/" + booking.getId() + "/success";
    }

    @GetMapping("/booking/{id}/success")
    public String bookingSuccess(@PathVariable Long id, Model model, @AuthenticationPrincipal AppUserDetails principal) {
        Booking booking = bookingService.getForCustomer(id, me(principal));
        model.addAttribute("booking", booking);
        model.addAttribute("payment", paymentRepository.findFirstByBookingAndSuccessTrueOrderByCreatedAtDesc(booking).orElse(null));
        return "booking/success";
    }

    @GetMapping("/booking/{id}/receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Booking booking = bookingService.getForCustomer(id, me(principal));
        if (booking.getAmountPaid().signum() <= 0) throw new BusinessException("A receipt is available only after payment.");
        return pdf(invoiceService.receipt(booking), "receipt-" + booking.getBookingRef() + ".pdf");
    }

    // =================================================================== US007 invoice

    @GetMapping("/bookings/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        Booking booking = bookingService.getForCustomer(id, me(principal));
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

    // =================================================================== US008 booking history

    @GetMapping("/bookings")
    public String history(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        List<Booking> all = bookingService.history(me(principal));
        model.addAttribute("upcoming", all.stream().filter(Booking::isUpcoming)
                .sorted(Comparator.comparing(Booking::getCheckInDate)).collect(Collectors.toList()));
        model.addAttribute("past", all.stream().filter(b -> !b.isUpcoming())
                .sorted(Comparator.comparing(Booking::getCheckInDate).reversed()).collect(Collectors.toList()));
        return "booking/history";
    }

    @GetMapping("/bookings/{id}")
    public String details(@PathVariable Long id, Model model, @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, me(principal));
        model.addAttribute("booking", b);
        model.addAttribute("canceled", bookingService.cancellationQuote(b));
        boolean modifiable = true;
        try {
            bookingService.checkModifiable(b);
        } catch (BusinessException e) {
            modifiable = false;
        }
        model.addAttribute("modifiable", modifiable);
        return "booking/details";
    }

    // =================================================================== US010 cancel

    @GetMapping("/bookings/{id}/cancel")
    public String cancelPage(@PathVariable Long id, Model model, RedirectAttributes ra, @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, me(principal));
        CancellationQuote q = bookingService.cancellationQuote(b);
        if (!q.isAllowed()) {
            error(ra, q.getBlockedReason());
            return "redirect:/customer/bookings/" + id;
        }
        model.addAttribute("booking", b);
        model.addAttribute("quote", q);
        return "booking/cancel";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra, @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, me(principal));
        try {
            bookingService.cancel(b, false);
        } catch (BusinessException e) {
            error(ra, e.getMessage());
            return "redirect:/customer/bookings/" + id;
        }
        if (b.getRefundAmount().signum() > 0) {
            success(ra, "Your booking has been canceled. A refund of " + BookingService.money(b.getRefundAmount())
                    + " will be processed within 3-5 business days.");
        } else {
            success(ra, "Your booking has been canceled. As per the hotel's policy, this booking is non-refundable.");
        }
        return "redirect:/customer/bookings/" + id;
    }

    // =================================================================== US009 modify

    @GetMapping("/bookings/{id}/modify")
    public String modifyForm(@PathVariable Long id, Model model, RedirectAttributes ra, @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, me(principal));
        try {
            bookingService.checkModifiable(b);
        } catch (BusinessException e) {
            error(ra, e.getMessage());
            return "redirect:/customer/bookings/" + id;
        }
        model.addAttribute("booking", b);
        model.addAttribute("form", bookingService.toModifyForm(b));
        model.addAttribute("today", java.time.LocalDate.now());
        return "booking/modify";
    }

    @PostMapping("/bookings/{id}/modify")
    public String modify(@PathVariable Long id, @ModelAttribute("form") ModifyBookingForm form, BindingResult errors,
                         Model model, HttpSession session, RedirectAttributes ra, @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, me(principal));
        ModificationDraft draft;
        try {
            draft = bookingService.prepareModification(b, form, errors);
        } catch (BusinessException e) {
            error(ra, e.getMessage());
            return "redirect:/customer/bookings/" + id;
        }
        if (draft == null) {
            model.addAttribute("booking", b);
            model.addAttribute("today", java.time.LocalDate.now());
            return "booking/modify";
        }
        session.setAttribute(MOD_DRAFT, draft);
        return "redirect:/customer/bookings/" + id + "/modify/review";
    }

    @GetMapping("/bookings/{id}/modify/review")
    public String modifyReview(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra,
                               @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, me(principal));
        ModificationDraft draft = (ModificationDraft) session.getAttribute(MOD_DRAFT);
        if (draft == null || !draft.getBookingId().equals(b.getId())) {
            error(ra, "Please choose the changes you want to make first.");
            return "redirect:/customer/bookings/" + id + "/modify";
        }
        model.addAttribute("booking", b);
        model.addAttribute("draft", draft);
        return "booking/modify-review";
    }

    @PostMapping("/bookings/{id}/modify/confirm")
    public String modifyConfirm(@PathVariable Long id, HttpSession session, RedirectAttributes ra,
                                @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, me(principal));
        ModificationDraft draft = (ModificationDraft) session.getAttribute(MOD_DRAFT);
        if (draft == null || !draft.getBookingId().equals(b.getId())) {
            error(ra, "Please choose the changes you want to make first.");
            return "redirect:/customer/bookings/" + id + "/modify";
        }
        if (draft.getDifference().signum() > 0) {
            session.setAttribute(PAYMENT_CTX, new PaymentContext(PaymentContext.MODIFICATION, b.getId(), draft.getDifference(), draft));
            return "redirect:/customer/payment";
        }
        try {
            bookingService.applyModification(b, draft, null);
        } catch (BusinessException e) {
            error(ra, e.getMessage());
            return "redirect:/customer/bookings/" + id + "/modify";
        }
        session.removeAttribute(MOD_DRAFT);
        if (draft.getDifference().signum() < 0) {
            success(ra, "Your booking was updated. Your updated booking costs less. Refund of "
                    + BookingService.money(draft.getDifference().negate()) + " will be processed within 3-5 business days.");
        } else {
            success(ra, "Your booking was updated successfully.");
        }
        return "redirect:/customer/bookings/" + id;
    }

    /** Real-time availability check used by the modify form while the customer changes dates or room type. */
    @GetMapping(value = "/bookings/{id}/availability", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> availability(@PathVariable Long id, @ModelAttribute ModifyBookingForm form,
                                            @AuthenticationPrincipal AppUserDetails principal) {
        Booking b = bookingService.getForCustomer(id, me(principal));
        BindingResult br = new BeanPropertyBindingResult(form, "form");
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
}

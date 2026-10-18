package com.hotel.booking.service;

import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.form.*;
import com.hotel.booking.model.*;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {

    /** Standard check-in time of the hotel; cancellation and modification windows are counted from it. */
    public static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final UserRepository users;
    private final RoomService roomService;
    private final BillService billService;
    private final NotificationService notifications;
    private final HotelProperties props;

    public BookingService(BookingRepository bookings, RoomRepository rooms, UserRepository users, RoomService roomService,
                          BillService billService, NotificationService notifications, HotelProperties props) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.users = users;
        this.roomService = roomService;
        this.billService = billService;
        this.notifications = notifications;
        this.props = props;
    }

    public static String money(BigDecimal v) {
        return "₹" + String.format(Locale.US, "%,.2f", v);
    }

    // ------------------------------------------------------------------ pricing

    private BigDecimal pct(BigDecimal value, BigDecimal percent) {
        return value.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public PriceQuote quote(Room room, LocalDate checkIn, LocalDate checkOut) {
        PriceQuote q = new PriceQuote();
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        q.setRoom(room);
        q.setCheckIn(checkIn);
        q.setCheckOut(checkOut);
        q.setNights(nights);
        q.setNightlyRate(room.getPricePerNight());
        BigDecimal base = room.getPricePerNight().multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
        q.setBase(base);
        q.setGst(pct(base, props.getGstPercent()));
        q.setService(pct(base, props.getServiceChargePercent()));
        q.setTotal(base.add(q.getGst()).add(q.getService()));
        BigDecimal factor = BigDecimal.ONE.add(props.getGstPercent().add(props.getServiceChargePercent())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        q.setNightlyRateWithTax(room.getPricePerNight().multiply(factor).setScale(2, RoundingMode.HALF_UP));
        return q;
    }

    private void applyQuote(Booking b, PriceQuote q) {
        b.setNightlyRate(q.getNightlyRate());
        b.setBaseAmount(q.getBase());
        b.setGstAmount(q.getGst());
        b.setServiceChargeAmount(q.getService());
        b.setTotalAmount(q.getTotal());
    }

    private LocalDateTime checkInMoment(Booking b) {
        return b.getCheckInDate().atTime(CHECK_IN_TIME);
    }

    // ------------------------------------------------------------------ lookups

    public Booking get(Long id) {
        return bookings.findById(id).orElseThrow(() -> new BusinessException("Booking not found."));
    }

    /** A customer can only ever see their own bookings. */
    public Booking getForCustomer(Long id, User customer) {
        Booking b = get(id);
        if (!b.getCustomer().getId().equals(customer.getId())) throw new BusinessException("Booking not found.");
        return b;
    }

    public List<Booking> history(User customer) {
        return bookings.findByCustomerOrderByCheckInDateDesc(customer);
    }

    private String newReference() {
        String ref;
        do {
            ref = "BK" + (10000000 + RANDOM.nextInt(90000000));
        } while (bookings.findByBookingRef(ref).isPresent());
        return ref;
    }

    // ------------------------------------------------------------------ new booking (US005 / US006)

    /** Creates the booking in "Pending Payment" state and holds the room while the customer pays. */
    @Transactional
    public Booking createPending(User customer, BookingRequestForm f) {
        Room room = roomService.get(f.getRoomId());
        int adults = f.getAdults();
        int children = f.getChildren() == null ? 0 : f.getChildren();
        if (adults + children > room.getMaxOccupancy()) {
            throw new BusinessException("Number of adults and children must not exceed the room occupancy limit (max "
                    + room.getMaxOccupancy() + ").");
        }
        if (!roomService.isAvailable(room, f.getCheckIn(), f.getCheckOut(), null)) {
            throw new BusinessException("Sorry, this room is no longer available for the selected dates. Please search again.");
        }
        Booking b = new Booking();
        b.setBookingRef(newReference());
        b.setCustomer(customer);
        b.setRoom(room);
        b.setCheckInDate(f.getCheckIn());
        b.setCheckOutDate(f.getCheckOut());
        b.setAdults(adults);
        b.setChildren(children);
        b.setSpecialRequests(StringUtils.hasText(f.getSpecialRequests()) ? f.getSpecialRequests().trim() : null);
        b.setPaymentMethod(f.getPaymentMethod());
        applyQuote(b, quote(room, f.getCheckIn(), f.getCheckOut()));
        b.setStatus(BookingStatus.PENDING_PAYMENT);
        b.setPaymentStatus(PaymentStatus.PENDING);
        b.setHoldExpiresAt(LocalDateTime.now().plusMinutes(props.getPaymentSessionMinutes()));
        return bookings.save(b);
    }

    /** Extends the hold when the customer returns to the payment screen; fails if somebody else took the room meanwhile. */
    @Transactional
    public Booking refreshHold(Booking b) {
        if (b.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("This booking is not waiting for payment.");
        }
        if (!roomService.isAvailable(b.getRoom(), b.getCheckInDate(), b.getCheckOutDate(), b.getId())) {
            b.setStatus(BookingStatus.CANCELLED);
            b.setCanceledAt(LocalDateTime.now());
            b.setHoldExpiresAt(null);
            bookings.save(b);
            throw new BusinessException("Sorry, this room is no longer available for the selected dates. Please search again.");
        }
        b.setHoldExpiresAt(LocalDateTime.now().plusMinutes(props.getPaymentSessionMinutes()));
        return bookings.save(b);
    }

    @Transactional
    public Booking markPaid(Booking b, Payment payment) {
        b.setStatus(BookingStatus.CONFIRMED);
        b.setPaymentStatus(PaymentStatus.PAID);
        b.setPaymentMethod(payment.getMethod());
        b.setAmountPaid(b.getTotalAmount());
        b.setHoldExpiresAt(null);
        bookings.save(b);
        billService.syncBookingBill(b);
        notifications.notify(b.getCustomer(), "Payment successful! Your booking " + b.getBookingRef() + " is confirmed for room "
                + b.getRoom().getRoomNumber() + ".");
        return b;
    }

    // ------------------------------------------------------------------ cancellation (US010)

    public CancellationQuote cancellationQuote(Booking b) {
        CancellationQuote q = new CancellationQuote();
        BookingStatus s = b.getEffectiveStatus();
        if (s == BookingStatus.CANCELLED) {
            q.setBlockedReason("This booking has already been canceled.");
            return q;
        }
        if (s == BookingStatus.CHECKED_IN) {
            q.setBlockedReason("This guest has already checked in, so the booking cannot be canceled.");
            return q;
        }
        if (s == BookingStatus.CHECKED_OUT || !checkInMoment(b).isAfter(LocalDateTime.now())) {
            q.setBlockedReason("This booking cannot be canceled as it is past the allowed cancellation window.");
            return q;
        }
        q.setAllowed(true);
        long hours = Duration.between(LocalDateTime.now(), checkInMoment(b)).toHours();
        BigDecimal paid = b.getAmountPaid() == null ? BigDecimal.ZERO : b.getAmountPaid();
        if (paid.signum() == 0) {
            q.setRefundPercent(0);
            q.setMessage("No payment has been collected for this booking, so there is nothing to refund.");
        } else if (hours >= props.getFreeCancellationHours()) {
            q.setRefundPercent(100);
            q.setRefundAmount(paid);
            q.setMessage("Free cancellation: you will receive a full refund of " + money(paid) + ". Do you want to proceed?");
        } else if (hours >= props.getPartialRefundHours()) {
            int p = props.getPartialRefundPercent();
            q.setRefundPercent(p);
            q.setRefundAmount(paid.multiply(BigDecimal.valueOf(p)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            q.setMessage("Canceling now will result in a " + p + "% refund (" + money(q.getRefundAmount())
                    + ") as per the hotel's cancellation policy. Do you want to proceed?");
        } else {
            q.setRefundPercent(0);
            q.setMessage("As per the hotel's policy, this booking is non-refundable. Do you want to proceed?");
        }
        return q;
    }

    @Transactional
    public Booking cancel(Booking b, boolean byAdmin) {
        CancellationQuote q;
        if (byAdmin) {
            // an admin-initiated cancellation is always refunded in full
            q = new CancellationQuote();
            q.setAllowed(b.getStatus() != BookingStatus.CANCELLED && b.getStatus() != BookingStatus.CHECKED_OUT);
            q.setBlockedReason(q.isAllowed() ? null : "This reservation is already " + b.getStatus().getLabel().toLowerCase() + ".");
            q.setRefundAmount(b.getAmountPaid());
        } else {
            q = cancellationQuote(b);
        }
        if (!q.isAllowed()) throw new BusinessException(q.getBlockedReason());
        b.setStatus(BookingStatus.CANCELLED);
        b.setCanceledAt(LocalDateTime.now());
        b.setHoldExpiresAt(null);
        BigDecimal refund = q.getRefundAmount() == null ? BigDecimal.ZERO : q.getRefundAmount();
        b.setRefundAmount(b.getRefundAmount().add(refund));
        if (refund.signum() > 0) b.setPaymentStatus(PaymentStatus.REFUNDED);
        if (b.getRoom().getStatus() == RoomStatus.OCCUPIED) {
            b.getRoom().setStatus(RoomStatus.AVAILABLE);
            rooms.save(b.getRoom());
        }
        bookings.save(b);
        billService.syncBookingBill(b);
        String msg = refund.signum() > 0
                ? "Your booking " + b.getBookingRef() + " has been canceled. A refund of " + money(refund) + " will be processed within 3-5 business days."
                : "Your booking " + b.getBookingRef() + " has been canceled. As per the hotel's policy, this booking is non-refundable.";
        notifications.notify(b.getCustomer(), msg);
        return b;
    }

    // ------------------------------------------------------------------ modification (US009)

    public void checkModifiable(Booking b) {
        if (b.getEffectiveStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only upcoming confirmed bookings can be modified.");
        }
        if (LocalDateTime.now().plusHours(props.getModificationCutoffHours()).isAfter(checkInMoment(b))) {
            throw new BusinessException("Modifications are not allowed within " + props.getModificationCutoffHours()
                    + " hours of check-in.");
        }
    }

    public ModifyBookingForm toModifyForm(Booking b) {
        ModifyBookingForm f = new ModifyBookingForm();
        f.setCheckIn(b.getCheckInDate());
        f.setCheckOut(b.getCheckOutDate());
        f.setAdults(b.getAdults());
        f.setChildren(b.getChildren());
        f.setRoomType(b.getRoom().getRoomType());
        return f;
    }

    /** Validates the requested changes, finds a room and prices the new stay. Returns null when there are errors. */
    public ModificationDraft prepareModification(Booking b, ModifyBookingForm f, BindingResult errors) {
        checkModifiable(b);
        StayValidator.validate(f.getCheckIn(), f.getCheckOut(), f.getAdults(), f.getChildren(), errors, false);
        if (f.getRoomType() == null) errors.rejectValue("roomType", "required", "Please select a room type.");
        if (errors.hasErrors()) return null;

        int adults = f.getAdults();
        int children = f.getChildren() == null ? 0 : f.getChildren();
        boolean unchanged = f.getCheckIn().equals(b.getCheckInDate()) && f.getCheckOut().equals(b.getCheckOutDate())
                && adults == b.getAdults() && children == b.getChildren() && f.getRoomType() == b.getRoom().getRoomType();
        if (unchanged) {
            errors.reject("nochange", "No changes were made to the booking.");
            return null;
        }
        if (LocalDateTime.now().plusHours(props.getModificationCutoffHours()).isAfter(f.getCheckIn().atTime(CHECK_IN_TIME))) {
            errors.rejectValue("checkIn", "cutoff", "The new check-in date must be at least " + props.getModificationCutoffHours()
                    + " hours from now.");
            return null;
        }

        Room current = b.getRoom();
        List<Room> candidates = rooms.findByRoomTypeAndStatusNot(f.getRoomType(), RoomStatus.UNDER_MAINTENANCE).stream()
                .sorted(Comparator.comparing((Room r) -> !r.getId().equals(current.getId())).thenComparing(Room::getPricePerNight))
                .collect(Collectors.toList());
        List<Room> bigEnough = candidates.stream().filter(r -> r.getMaxOccupancy() >= adults + children).collect(Collectors.toList());
        if (bigEnough.isEmpty()) {
            int max = candidates.stream().mapToInt(Room::getMaxOccupancy).max().orElse(0);
            errors.rejectValue("children", "capacity", "Number of adults and children must not exceed the room occupancy limit"
                    + (max > 0 ? " (max " + max + " for this room type)." : "."));
            return null;
        }
        Room target = bigEnough.stream().filter(r -> roomService.isAvailable(r, f.getCheckIn(), f.getCheckOut(), b.getId()))
                .findFirst().orElse(null);
        if (target == null) {
            errors.rejectValue("roomType", "unavailable",
                    "The selected room type is fully booked for these dates. Please choose different dates or another room type.");
            return null;
        }

        PriceQuote q = quote(target, f.getCheckIn(), f.getCheckOut());
        ModificationDraft d = new ModificationDraft();
        d.setBookingId(b.getId());
        d.setRoomId(target.getId());
        d.setRoomNumber(target.getRoomNumber());
        d.setRoomType(target.getRoomType());
        d.setCheckIn(f.getCheckIn());
        d.setCheckOut(f.getCheckOut());
        d.setAdults(adults);
        d.setChildren(children);
        d.setNights(q.getNights());
        d.setNightlyRate(q.getNightlyRate());
        d.setBase(q.getBase());
        d.setGst(q.getGst());
        d.setService(q.getService());
        d.setTotal(q.getTotal());
        d.setDifference(q.getTotal().subtract(b.getTotalAmount()));
        return d;
    }

    /** Applies a confirmed modification. {@code extraPayment} is the successful payment of the price difference, if any. */
    @Transactional
    public Booking applyModification(Booking b, ModificationDraft d, Payment extraPayment) {
        checkModifiable(b);
        Room target = roomService.get(d.getRoomId());
        if (!roomService.isAvailable(target, d.getCheckIn(), d.getCheckOut(), b.getId())) {
            throw new BusinessException("The selected room is no longer available for these dates. Please try again.");
        }
        BigDecimal diff = d.getTotal().subtract(b.getTotalAmount());
        if (diff.signum() > 0 && (extraPayment == null || !extraPayment.isSuccess())) {
            throw new BusinessException("The additional payment is required before the booking can be modified.");
        }
        b.setRoom(target);
        b.setCheckInDate(d.getCheckIn());
        b.setCheckOutDate(d.getCheckOut());
        b.setAdults(d.getAdults());
        b.setChildren(d.getChildren());
        b.setNightlyRate(d.getNightlyRate());
        b.setBaseAmount(d.getBase());
        b.setGstAmount(d.getGst());
        b.setServiceChargeAmount(d.getService());
        b.setTotalAmount(d.getTotal());
        if (diff.signum() < 0) b.setRefundAmount(b.getRefundAmount().add(diff.negate()));
        b.setAmountPaid(d.getTotal());
        b.setPaymentStatus(PaymentStatus.PAID);
        bookings.save(b);
        billService.syncBookingBill(b);
        String msg = diff.signum() < 0
                ? "Your booking " + b.getBookingRef() + " was updated. Your updated booking costs less. Refund of " + money(diff.negate())
                + " will be processed within 3-5 business days."
                : "Your booking " + b.getBookingRef() + " was updated successfully.";
        notifications.notify(b.getCustomer(), msg);
        return b;
    }

    // ------------------------------------------------------------------ admin reservations (US017 / US018)

    public List<Booking> filter(ReservationFilter f) {
        String id = lower(f.getReservationId());
        String customer = lower(f.getCustomer());
        String room = lower(f.getRoomNumber());
        return bookings.findAllByOrderByCreatedAtDesc().stream()
                .filter(b -> id.isEmpty() || b.getBookingRef().toLowerCase(Locale.ROOT).contains(id))
                .filter(b -> customer.isEmpty() || b.getCustomer().getFullName().toLowerCase(Locale.ROOT).contains(customer)
                        || b.getCustomer().getUsername().toLowerCase(Locale.ROOT).contains(customer))
                .filter(b -> room.isEmpty() || b.getRoom().getRoomNumber().toLowerCase(Locale.ROOT).contains(room))
                .filter(b -> f.getCheckIn() == null || !b.getCheckInDate().isBefore(f.getCheckIn()))
                .filter(b -> f.getCheckOut() == null || !b.getCheckOutDate().isAfter(f.getCheckOut()))
                .filter(b -> f.getStatus() == null || b.getEffectiveStatus() == f.getStatus())
                .filter(b -> f.getRoomType() == null || b.getRoom().getRoomType() == f.getRoomType())
                .filter(b -> f.getBookingDate() == null || b.getCreatedAt().toLocalDate().equals(f.getBookingDate()))
                .collect(Collectors.toList());
    }

    private static String lower(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public AdminReservationForm toAdminForm(Booking b) {
        AdminReservationForm f = new AdminReservationForm();
        f.setRoomId(b.getRoom().getId());
        f.setCheckIn(b.getCheckInDate());
        f.setCheckOut(b.getCheckOutDate());
        f.setAdults(b.getAdults());
        f.setChildren(b.getChildren());
        f.setNotes(b.getAdminNotes() != null ? b.getAdminNotes() : b.getSpecialRequests());
        f.setStatus(b.getStatus() == BookingStatus.PENDING_PAYMENT || b.getStatus() == BookingStatus.CANCELLED ? null : b.getEffectiveStatus());
        return f;
    }

    public void validateAdminReservation(AdminReservationForm f, Booking existing, BindingResult errors) {
        boolean keepsStarted = existing != null && existing.getCheckInDate().equals(f.getCheckIn());
        StayValidator.validate(f.getCheckIn(), f.getCheckOut(), f.getAdults(), f.getChildren(), errors, keepsStarted);
        if (existing == null && !StringUtils.hasText(f.getCustomerUsername())) {
            errors.rejectValue("customerUsername", "required", "Enter the customer's username.");
        } else if (existing == null && users.findByUsername(f.getCustomerUsername().trim()).isEmpty()) {
            errors.rejectValue("customerUsername", "unknown", "No customer found with this username.");
        }
        if (f.getRoomId() == null) {
            errors.rejectValue("roomId", "required", "Select a room.");
        }
        if (errors.hasErrors()) return;
        Room room = roomService.get(f.getRoomId());
        int guests = f.getAdults() + (f.getChildren() == null ? 0 : f.getChildren());
        if (guests > room.getMaxOccupancy()) {
            errors.rejectValue("roomId", "capacity", "Room " + room.getRoomNumber() + " holds at most " + room.getMaxOccupancy() + " guests.");
        } else if (!roomService.isAvailable(room, f.getCheckIn(), f.getCheckOut(), existing == null ? null : existing.getId())) {
            errors.rejectValue("roomId", "conflict",
                    "Room " + room.getRoomNumber() + " conflicts with an existing reservation or is under maintenance for these dates.");
        }
    }

    @Transactional
    public Booking adminCreate(AdminReservationForm f) {
        User customer = users.findByUsername(f.getCustomerUsername().trim()).orElseThrow();
        Room room = roomService.get(f.getRoomId());
        Booking b = new Booking();
        b.setBookingRef(newReference());
        b.setCustomer(customer);
        b.setRoom(room);
        b.setCheckInDate(f.getCheckIn());
        b.setCheckOutDate(f.getCheckOut());
        b.setAdults(f.getAdults());
        b.setChildren(f.getChildren() == null ? 0 : f.getChildren());
        b.setAdminNotes(StringUtils.hasText(f.getNotes()) ? f.getNotes().trim() : null);
        applyQuote(b, quote(room, f.getCheckIn(), f.getCheckOut()));
        b.setStatus(BookingStatus.CONFIRMED);
        b.setPaymentStatus(PaymentStatus.PENDING);
        bookings.save(b);
        billService.syncBookingBill(b);
        notifications.notify(customer, "A reservation " + b.getBookingRef() + " was created for you for room " + room.getRoomNumber() + ".");
        return b;
    }

    @Transactional
    public Booking adminUpdate(Booking b, AdminReservationForm f) {
        if (b.getStatus() == BookingStatus.CANCELLED) throw new BusinessException("A canceled reservation cannot be edited.");
        Room room = roomService.get(f.getRoomId());
        b.setRoom(room);
        b.setCheckInDate(f.getCheckIn());
        b.setCheckOutDate(f.getCheckOut());
        b.setAdults(f.getAdults());
        b.setChildren(f.getChildren() == null ? 0 : f.getChildren());
        b.setAdminNotes(StringUtils.hasText(f.getNotes()) ? f.getNotes().trim() : null);
        PriceQuote q = quote(room, f.getCheckIn(), f.getCheckOut());
        applyQuote(b, q);
        // keep the payment status honest after a price change
        if (b.getAmountPaid().signum() > 0) {
            int cmp = b.getAmountPaid().compareTo(q.getTotal());
            if (cmp < 0) {
                b.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
            } else {
                if (cmp > 0) {
                    b.setRefundAmount(b.getRefundAmount().add(b.getAmountPaid().subtract(q.getTotal())));
                    b.setAmountPaid(q.getTotal());
                }
                b.setPaymentStatus(PaymentStatus.PAID);
            }
        }
        BookingStatus wanted = f.getStatus();
        if (wanted != null && b.getStatus() != BookingStatus.PENDING_PAYMENT) {
            b.setStatus(wanted);
            if (wanted == BookingStatus.CHECKED_IN) {
                room.setStatus(RoomStatus.OCCUPIED);
                rooms.save(room);
            } else if (wanted == BookingStatus.CHECKED_OUT && room.getStatus() == RoomStatus.OCCUPIED) {
                room.setStatus(RoomStatus.AVAILABLE);
                rooms.save(room);
            }
        }
        bookings.save(b);
        billService.syncBookingBill(b);
        notifications.notify(b.getCustomer(), "Your reservation " + b.getBookingRef() + " was updated by the hotel.");
        return b;
    }
}

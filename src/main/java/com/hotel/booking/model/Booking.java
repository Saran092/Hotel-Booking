package com.hotel.booking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String bookingRef;

    @ManyToOne(optional = false)
    private User customer;

    @ManyToOne(optional = false)
    private Room room;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private int adults = 1;

    private int children = 0;

    @Column(length = 500)
    private String specialRequests;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(precision = 10, scale = 2)
    private BigDecimal nightlyRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal serviceChargeAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime canceledAt;

    private LocalDateTime holdExpiresAt;

    @Column(length = 500)
    private String adminNotes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public int getAdults() { return adults; }
    public void setAdults(int adults) { this.adults = adults; }

    public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getNightlyRate() { return nightlyRate; }
    public void setNightlyRate(BigDecimal nightlyRate) { this.nightlyRate = nightlyRate; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getServiceChargeAmount() { return serviceChargeAmount; }
    public void setServiceChargeAmount(BigDecimal serviceChargeAmount) { this.serviceChargeAmount = serviceChargeAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCanceledAt() { return canceledAt; }
    public void setCanceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; }

    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public int getNights() {
        if (checkInDate == null || checkOutDate == null) return 0;
        return (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public int getGuests() { return adults + children; }

    /** A confirmed stay whose check-out date has passed is shown as Checked-out. */
    public BookingStatus getEffectiveStatus() {
        if (status == BookingStatus.CONFIRMED && checkOutDate != null && checkOutDate.isBefore(LocalDate.now())) {
            return BookingStatus.CHECKED_OUT;
        }
        return status;
    }

    /** Upcoming = still active and not finished yet. Everything else is shown under Past Bookings. */
    public boolean isUpcoming() {
        BookingStatus s = getEffectiveStatus();
        return (s == BookingStatus.PENDING_PAYMENT || s == BookingStatus.CONFIRMED || s == BookingStatus.CHECKED_IN)
                && checkOutDate != null && !checkOutDate.isBefore(LocalDate.now());
    }

    public boolean isCancelled() { return status == BookingStatus.CANCELLED; }

    public boolean isPaid() { return paymentStatus == PaymentStatus.PAID && status != BookingStatus.CANCELLED; }

}

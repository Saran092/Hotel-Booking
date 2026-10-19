package com.hotel.booking.api.dto;

import com.hotel.booking.model.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingDto(Long id, String bookingRef, RoomDto room, UserSummary customer,
                         LocalDate checkInDate, LocalDate checkOutDate, int nights, int adults, int children,
                         String specialRequests, String adminNotes, String paymentMethod,
                         BigDecimal nightlyRate, BigDecimal baseAmount, BigDecimal gstAmount,
                         BigDecimal serviceChargeAmount, BigDecimal totalAmount, BigDecimal amountPaid,
                         BigDecimal refundAmount, String status, String effectiveStatus, String paymentStatus,
                         boolean upcoming, LocalDateTime createdAt, LocalDateTime canceledAt, LocalDateTime holdExpiresAt) {

    public static BookingDto of(Booking b) {
        return new BookingDto(b.getId(), b.getBookingRef(), RoomDto.of(b.getRoom()),
                b.getCustomer() == null ? null : UserSummary.brief(b.getCustomer()),
                b.getCheckInDate(), b.getCheckOutDate(), b.getNights(), b.getAdults(), b.getChildren(),
                b.getSpecialRequests(), b.getAdminNotes(),
                b.getPaymentMethod() == null ? null : b.getPaymentMethod().name(),
                b.getNightlyRate(), b.getBaseAmount(), b.getGstAmount(), b.getServiceChargeAmount(),
                b.getTotalAmount(), b.getAmountPaid(), b.getRefundAmount(),
                b.getStatus().name(), b.getEffectiveStatus().name(), b.getPaymentStatus().name(),
                b.isUpcoming(), b.getCreatedAt(), b.getCanceledAt(), b.getHoldExpiresAt());
    }
}

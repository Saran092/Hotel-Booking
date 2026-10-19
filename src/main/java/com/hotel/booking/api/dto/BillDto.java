package com.hotel.booking.api.dto;

import com.hotel.booking.model.Bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BillDto(Long id, String billRef, UserSummary customer, Long bookingId, String bookingRef,
                      LocalDateTime issueDate, String paymentStatus, BigDecimal totalAmount, String notes,
                      List<BillItemDto> items) {

    public static BillDto of(Bill b) {
        return new BillDto(b.getId(), b.getBillRef(), UserSummary.brief(b.getCustomer()),
                b.getBooking() == null ? null : b.getBooking().getId(),
                b.getBooking() == null ? null : b.getBooking().getBookingRef(),
                b.getIssueDate(), b.getPaymentStatus().name(), b.getTotalAmount(), b.getNotes(),
                b.getItems().stream().map(BillItemDto::of).toList());
    }
}

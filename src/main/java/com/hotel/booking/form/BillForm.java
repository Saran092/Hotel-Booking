package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class BillForm {

    private Long customerId;

    private Long bookingId;

    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Size(max = 500, message = "Notes cannot exceed 500 characters.")
    private String notes;

    private List<BillItemForm> items = new ArrayList<>();

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<BillItemForm> getItems() { return items; }
    public void setItems(List<BillItemForm> items) { this.items = items; }

}

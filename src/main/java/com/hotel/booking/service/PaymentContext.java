package com.hotel.booking.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentContext implements Serializable {

    private String type;

    private Long bookingId;

    private BigDecimal amount;

    private LocalDateTime createdAt = LocalDateTime.now();

    private ModificationDraft draft;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public ModificationDraft getDraft() { return draft; }
    public void setDraft(ModificationDraft draft) { this.draft = draft; }

    private static final long serialVersionUID = 1L;

    public static final String BOOKING = "BOOKING";
    public static final String MODIFICATION = "MODIFICATION";

    public PaymentContext() { }

    public PaymentContext(String type, Long bookingId, BigDecimal amount, ModificationDraft draft) {
        this.type = type;
        this.bookingId = bookingId;
        this.amount = amount;
        this.draft = draft;
    }

    public boolean isExpired(int minutes) { return createdAt.plusMinutes(minutes).isBefore(LocalDateTime.now()); }

    public void touch() { this.createdAt = LocalDateTime.now(); }

    public boolean isModification() { return MODIFICATION.equals(type); }

}

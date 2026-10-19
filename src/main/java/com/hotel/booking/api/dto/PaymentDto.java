package com.hotel.booking.api.dto;

import com.hotel.booking.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDto(Long id, String transactionId, BigDecimal amount, String method, String purpose,
                         boolean success, String failureReason, String cardLast4, String payerName,
                         LocalDateTime createdAt) {

    public static PaymentDto of(Payment p) {
        if (p == null) return null;
        return new PaymentDto(p.getId(), p.getTransactionId(), p.getAmount(),
                p.getMethod() == null ? null : p.getMethod().name(), p.getPurpose(), p.isSuccess(),
                p.getFailureReason(), p.getCardLast4(), p.getPayerName(), p.getCreatedAt());
    }
}

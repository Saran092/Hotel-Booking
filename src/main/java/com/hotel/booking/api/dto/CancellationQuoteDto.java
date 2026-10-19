package com.hotel.booking.api.dto;

import com.hotel.booking.service.CancellationQuote;

import java.math.BigDecimal;

public record CancellationQuoteDto(boolean allowed, String blockedReason, int refundPercent,
                                   BigDecimal refundAmount, String message) {

    public static CancellationQuoteDto of(CancellationQuote q) {
        return new CancellationQuoteDto(q.isAllowed(), q.getBlockedReason(), q.getRefundPercent(),
                q.getRefundAmount(), q.getMessage());
    }
}

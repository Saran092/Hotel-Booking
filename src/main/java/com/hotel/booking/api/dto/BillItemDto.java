package com.hotel.booking.api.dto;

import com.hotel.booking.model.BillItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillItemDto(Long id, String category, String description, LocalDateTime serviceDateTime,
                          int quantity, BigDecimal unitPrice, BigDecimal taxPercent, BigDecimal discount,
                          BigDecimal lineTotal) {

    public static BillItemDto of(BillItem i) {
        return new BillItemDto(i.getId(), i.getCategory().name(), i.getDescription(), i.getServiceDateTime(),
                i.getQuantity(), i.getUnitPrice(), i.getTaxPercent(), i.getDiscount(), i.getLineTotal());
    }
}

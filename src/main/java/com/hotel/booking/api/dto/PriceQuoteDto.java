package com.hotel.booking.api.dto;

import com.hotel.booking.service.PriceQuote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceQuoteDto(RoomDto room, LocalDate checkIn, LocalDate checkOut, int nights,
                            BigDecimal nightlyRate, BigDecimal nightlyRateWithTax,
                            BigDecimal base, BigDecimal gst, BigDecimal service, BigDecimal total) {

    public static PriceQuoteDto of(PriceQuote q) {
        return new PriceQuoteDto(RoomDto.of(q.getRoom()), q.getCheckIn(), q.getCheckOut(), q.getNights(),
                q.getNightlyRate(), q.getNightlyRateWithTax(), q.getBase(), q.getGst(), q.getService(), q.getTotal());
    }
}

package com.hotel.booking.service;

import com.hotel.booking.form.RoomSearchForm;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StayValidatorTest {

    private BeanPropertyBindingResult run(LocalDate in, LocalDate out, Integer adults, Integer children) {
        BeanPropertyBindingResult r = new BeanPropertyBindingResult(new RoomSearchForm(), "form");
        StayValidator.validate(in, out, adults, children, r, false);
        return r;
    }

    @Test
    void acceptsAValidStay() {
        LocalDate today = LocalDate.now();
        assertFalse(run(today.plusDays(1), today.plusDays(3), 2, 1).hasErrors());
    }

    @Test
    void rejectsPastCheckInAndBadOrder() {
        LocalDate today = LocalDate.now();
        assertTrue(run(today.minusDays(1), today.plusDays(1), 1, 0).hasFieldErrors("checkIn"));
        assertTrue(run(today.plusDays(3), today.plusDays(3), 1, 0).hasFieldErrors("checkOut"));
    }

    @Test
    void rejectsZeroAdults() {
        LocalDate today = LocalDate.now();
        assertTrue(run(today.plusDays(1), today.plusDays(2), 0, 0).hasFieldErrors("adults"));
    }
}

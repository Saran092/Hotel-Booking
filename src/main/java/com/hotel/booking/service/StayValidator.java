package com.hotel.booking.service;

import org.springframework.validation.BindingResult;

import java.time.LocalDate;

/** Validation shared by every screen that asks for check-in / check-out dates and guest counts. */
public final class StayValidator {

    public static final int MAX_ADULTS = 10;
    public static final int MAX_CHILDREN = 5;

    private StayValidator() {
    }

    /**
     * @param allowPastCheckIn true for admin screens that edit a stay which has already started
     */
    public static void validate(LocalDate checkIn, LocalDate checkOut, Integer adults, Integer children,
                                BindingResult errors, boolean allowPastCheckIn) {
        if (checkIn == null) {
            errors.rejectValue("checkIn", "required", "Check-in date is required.");
        } else if (!allowPastCheckIn && checkIn.isBefore(LocalDate.now())) {
            errors.rejectValue("checkIn", "past", "Check-in date cannot be in the past.");
        }
        if (checkOut == null) {
            errors.rejectValue("checkOut", "required", "Check-out date is required.");
        } else if (checkIn != null && !checkOut.isAfter(checkIn)) {
            errors.rejectValue("checkOut", "order", "Check-out date must be after the check-in date.");
        }
        if (adults == null || adults < 1) {
            errors.rejectValue("adults", "min", "At least one adult must be selected.");
        } else if (adults > MAX_ADULTS) {
            errors.rejectValue("adults", "max", "Number of adults cannot exceed " + MAX_ADULTS + ".");
        }
        if (children != null && children < 0) {
            errors.rejectValue("children", "negative", "Number of children cannot be negative.");
        } else if (children != null && children > MAX_CHILDREN) {
            errors.rejectValue("children", "max", "Number of children cannot exceed " + MAX_CHILDREN + ".");
        }
    }
}

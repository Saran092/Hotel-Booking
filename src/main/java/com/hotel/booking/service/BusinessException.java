package com.hotel.booking.service;

/** A rule of the hotel was violated; the message is safe to show to the user as-is. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

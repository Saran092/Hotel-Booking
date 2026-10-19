package com.hotel.booking.security;

import java.util.LinkedHashMap;
import java.util.Map;

/** Field-level validation failures, rendered as a 400 JSON body. */
public class ApiValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ApiValidationException(Map<String, String> errors) {
        super("Please correct the errors below.");
        this.errors = new LinkedHashMap<>(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}

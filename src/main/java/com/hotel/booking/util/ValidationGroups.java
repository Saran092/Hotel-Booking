package com.hotel.booking.util;

import jakarta.validation.GroupSequence;

/** "Required" checks run first so that an empty field shows the "required" message instead of a format message. */
public final class ValidationGroups {

    private ValidationGroups() {
    }

    public interface Required {
    }

    public interface Format {
    }

    @GroupSequence({Required.class, Format.class})
    public interface Ordered {
    }
}

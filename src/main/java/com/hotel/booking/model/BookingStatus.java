package com.hotel.booking.model;

public enum BookingStatus {

    PENDING_PAYMENT("Pending Payment"),
    CONFIRMED("Confirmed"),
    CHECKED_IN("Checked-in"),
    CHECKED_OUT("Checked-out"),
    CANCELLED("Canceled");

    private final String label;

    BookingStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

}

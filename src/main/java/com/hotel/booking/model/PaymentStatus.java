package com.hotel.booking.model;

public enum PaymentStatus {

    PENDING("Pending"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Paid"),
    REFUNDED("Refunded");

    private final String label;

    PaymentStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

}

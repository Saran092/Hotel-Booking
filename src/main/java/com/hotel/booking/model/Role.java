package com.hotel.booking.model;

public enum Role {

    CUSTOMER("Customer"),
    ADMIN("Admin"),
    STAFF("Staff");

    private final String label;

    Role(String label) { this.label = label; }

    public String getLabel() { return label; }

}

package com.hotel.booking.model;

public enum StaffExpertise {

    BILLING("Billing"),
    MAINTENANCE("Maintenance"),
    HOUSEKEEPING("Housekeeping"),
    GENERAL("General");

    private final String label;

    StaffExpertise(String label) { this.label = label; }

    public String getLabel() { return label; }

}

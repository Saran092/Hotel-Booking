package com.hotel.booking.model;

public enum ContactPreference {

    CALL("Call"),
    EMAIL("Email");

    private final String label;

    ContactPreference(String label) { this.label = label; }

    public String getLabel() { return label; }

}

package com.hotel.booking.model;

public enum RoomType {

    SINGLE("Single"),
    DOUBLE("Double"),
    STANDARD("Standard"),
    DELUXE("Deluxe"),
    SUITE("Suite"),
    SUPREME("Supreme");

    private final String label;

    RoomType(String label) { this.label = label; }

    public String getLabel() { return label; }

}

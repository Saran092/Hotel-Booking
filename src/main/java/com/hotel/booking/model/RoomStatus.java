package com.hotel.booking.model;

public enum RoomStatus {

    AVAILABLE("Available"),
    OCCUPIED("Occupied"),
    UNDER_MAINTENANCE("Under Maintenance");

    private final String label;

    RoomStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

}

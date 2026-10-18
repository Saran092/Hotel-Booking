package com.hotel.booking.model;

public enum ComplaintCategory {

    ROOM_ISSUE("Room Issue"),
    SERVICE_ISSUE("Service Issue"),
    BILLING_ISSUE("Billing Issue"),
    OTHER("Other");

    private final String label;

    ComplaintCategory(String label) { this.label = label; }

    public String getLabel() { return label; }

}

package com.hotel.booking.model;

public enum ComplaintPriority {

    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String label;

    ComplaintPriority(String label) { this.label = label; }

    public String getLabel() { return label; }

}

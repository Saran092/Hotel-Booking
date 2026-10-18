package com.hotel.booking.model;

public enum ComplaintStatus {

    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    ESCALATED("Escalated");

    private final String label;

    ComplaintStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

}

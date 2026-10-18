package com.hotel.booking.model;

public enum ActionType {

    STEP("Step implemented"),
    COMMUNICATION("Communication with customer"),
    INTERNAL_NOTE("Internal note"),
    INVESTIGATION("Investigation"),
    RESOLUTION("Resolution"),
    STATUS_CHANGE("Status change");

    private final String label;

    ActionType(String label) { this.label = label; }

    public String getLabel() { return label; }

}

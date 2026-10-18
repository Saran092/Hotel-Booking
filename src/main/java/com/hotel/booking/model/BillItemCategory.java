package com.hotel.booking.model;

public enum BillItemCategory {

    ROOM("Room Charges"),
    SERVICE("Service Charges"),
    ADDITIONAL("Additional Fees");

    private final String label;

    BillItemCategory(String label) { this.label = label; }

    public String getLabel() { return label; }

}

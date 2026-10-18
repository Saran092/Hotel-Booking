package com.hotel.booking.model;

public enum PaymentMethod {

    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    UPI("UPI"),
    WALLET("Wallet"),
    NET_BANKING("Net Banking");

    private final String label;

    PaymentMethod(String label) { this.label = label; }

    public String getLabel() { return label; }

}

package com.hotel.booking.util;

/**
 * Validation patterns shared by the server-side Bean Validation annotations and the
 * browser-side validation (templates read them through T(...) so there is a single source of truth).
 */
public final class Patterns {

    private Patterns() {
    }

    public static final String NAME = "^(?=.*[A-Za-z].*[A-Za-z].*[A-Za-z])[A-Za-z ]+$";
    public static final String PROFILE_NAME = "^(?=.*[A-Za-z].*[A-Za-z])[A-Za-z ]{2,50}$";
    public static final String EMAIL = "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$";
    public static final String MOBILE = "^\\d{8,10}$";
    public static final String USERNAME = "^\\S{5,60}$";
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,}$";
    public static final String CARD_HOLDER = "^(?=.*[A-Za-z].*[A-Za-z].*[A-Za-z])[A-Za-z ]{3,50}$";
    public static final String CARD_NUMBER = "^\\d{16}$";
    public static final String CVV = "^\\d{3,4}$";
    public static final String EXPIRY = "^(0[1-9]|1[0-2])/\\d{2}$";
    public static final String UPI = "^[A-Za-z0-9._\\-]{2,}@[A-Za-z]{2,}$";
    public static final String SEARCH_TEXT = "^[A-Za-z0-9 \\-']*$";

    public static final String MSG_NAME = "Name must be at least 3 characters long and contain only letters.";
    public static final String MSG_EMAIL = "Enter a valid email address.";
    public static final String MSG_MOBILE = "Enter a valid mobile number.";
    public static final String MSG_ADDRESS = "Address must be at least 10 characters long.";
    public static final String MSG_USERNAME = "Username must be at least 5 characters and unique.";
    public static final String MSG_PASSWORD = "Password must be at least 8 characters and include a mix of uppercase, lowercase, number, and special character.";
}

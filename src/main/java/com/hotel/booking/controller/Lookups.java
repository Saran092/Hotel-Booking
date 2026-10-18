package com.hotel.booking.controller;

import java.util.LinkedHashMap;
import java.util.Map;

/** Static drop-down data. */
public final class Lookups {

    private Lookups() {
    }

    public static final Map<String, String> COUNTRY_CODES = new LinkedHashMap<>();

    static {
        COUNTRY_CODES.put("+91", "+91 India");
        COUNTRY_CODES.put("+1", "+1 USA / Canada");
        COUNTRY_CODES.put("+44", "+44 United Kingdom");
        COUNTRY_CODES.put("+61", "+61 Australia");
        COUNTRY_CODES.put("+65", "+65 Singapore");
        COUNTRY_CODES.put("+971", "+971 UAE");
        COUNTRY_CODES.put("+966", "+966 Saudi Arabia");
        COUNTRY_CODES.put("+974", "+974 Qatar");
        COUNTRY_CODES.put("+60", "+60 Malaysia");
        COUNTRY_CODES.put("+94", "+94 Sri Lanka");
        COUNTRY_CODES.put("+977", "+977 Nepal");
        COUNTRY_CODES.put("+880", "+880 Bangladesh");
        COUNTRY_CODES.put("+49", "+49 Germany");
        COUNTRY_CODES.put("+33", "+33 France");
        COUNTRY_CODES.put("+81", "+81 Japan");
    }
}

package com.hotel.booking.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternsTest {

    @Test
    void passwordNeedsAllCharacterClasses() {
        assertTrue("Welcome@123".matches(Patterns.PASSWORD));
        assertFalse("welcome@123".matches(Patterns.PASSWORD));   // no upper case
        assertFalse("WELCOME@123".matches(Patterns.PASSWORD));   // no lower case
        assertFalse("Welcome@abc".matches(Patterns.PASSWORD));   // no digit
        assertFalse("Welcome123".matches(Patterns.PASSWORD));    // no special character
        assertFalse("We@1".matches(Patterns.PASSWORD));          // too short
    }

    @Test
    void mobileIsEightToTenDigits() {
        assertTrue("98765432".matches(Patterns.MOBILE));
        assertTrue("9876543210".matches(Patterns.MOBILE));
        assertFalse("9876543".matches(Patterns.MOBILE));
        assertFalse("98765432101".matches(Patterns.MOBILE));
        assertFalse("98765abc10".matches(Patterns.MOBILE));
    }

    @Test
    void emailAndCardPatterns() {
        assertTrue("guest@example.com".matches(Patterns.EMAIL));
        assertFalse("guest@example".matches(Patterns.EMAIL));
        assertTrue("4111111111111111".matches(Patterns.CARD_NUMBER));
        assertFalse("4111 1111 1111 1111".matches(Patterns.CARD_NUMBER));
        assertTrue("12/30".matches(Patterns.EXPIRY));
        assertFalse("13/30".matches(Patterns.EXPIRY));
        assertTrue("name@bank".matches(Patterns.UPI));
    }

    @Test
    void nameNeedsThreeLetters() {
        assertTrue("Anna Lee".matches(Patterns.NAME));
        assertFalse("Al".matches(Patterns.NAME));
        assertFalse("Anna 99".matches(Patterns.NAME));
    }
}

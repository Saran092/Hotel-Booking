package com.hotel.booking.service;

import com.hotel.booking.util.Patterns;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    @Test
    void generatedTemporaryPasswordsMeetThePasswordPolicy() {
        for (int i = 0; i < 200; i++) {
            String pw = UserService.generatePassword();
            assertTrue(pw.matches(Patterns.PASSWORD), "Password violates policy: " + pw);
        }
    }
}

package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class ForgotPasswordForm {

    @NotBlank(message = "Enter your username.")
    private String username = "";

    @Pattern(regexp = Patterns.EMAIL, message = Patterns.MSG_EMAIL)
    private String email = "";

    @Pattern(regexp = Patterns.PASSWORD, message = Patterns.MSG_PASSWORD)
    private String newPassword = "";

    @NotBlank(message = "Passwords do not match.")
    private String confirmPassword = "";

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

}

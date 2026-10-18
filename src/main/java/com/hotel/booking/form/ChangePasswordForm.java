package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class ChangePasswordForm {

    @Pattern(regexp = Patterns.PASSWORD, message = Patterns.MSG_PASSWORD)
    private String newPassword = "";

    @NotBlank(message = "Passwords do not match.")
    private String confirmPassword = "";

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

}

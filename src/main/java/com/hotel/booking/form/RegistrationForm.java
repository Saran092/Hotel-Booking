package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class RegistrationForm {

    @Pattern(regexp = Patterns.NAME, message = Patterns.MSG_NAME)
    private String fullName = "";

    @Pattern(regexp = Patterns.EMAIL, message = Patterns.MSG_EMAIL)
    @Size(max = 150, message = Patterns.MSG_EMAIL)
    private String email = "";

    @NotBlank(message = "Select a country code.")
    private String countryCode = "+91";

    @Pattern(regexp = Patterns.MOBILE, message = Patterns.MSG_MOBILE)
    private String mobile = "";

    @NotBlank(message = Patterns.MSG_ADDRESS)
    @Size(min = 10, max = 255, message = Patterns.MSG_ADDRESS)
    private String address = "";

    @Pattern(regexp = Patterns.USERNAME, message = Patterns.MSG_USERNAME)
    private String username = "";

    @Pattern(regexp = Patterns.PASSWORD, message = Patterns.MSG_PASSWORD)
    private String password = "";

    @NotBlank(message = "Passwords do not match.")
    private String confirmPassword = "";

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

}

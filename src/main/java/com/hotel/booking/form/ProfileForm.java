package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class ProfileForm {

    @NotBlank(message = "Please fill in all required fields.", groups = ValidationGroups.Required.class)
    @Pattern(regexp = Patterns.PROFILE_NAME, message = "Full name must be 2-50 letters; numbers and special characters are not allowed.", groups = ValidationGroups.Format.class)
    private String fullName;

    @NotBlank(message = "Please fill in all required fields.", groups = ValidationGroups.Required.class)
    @Pattern(regexp = Patterns.EMAIL, message = "Please enter a valid email address.", groups = ValidationGroups.Format.class)
    private String email;

    @NotBlank(message = "Please fill in all required fields.", groups = ValidationGroups.Required.class)
    private String countryCode;

    @NotBlank(message = "Please fill in all required fields.", groups = ValidationGroups.Required.class)
    @Pattern(regexp = Patterns.MOBILE, message = "Please enter a valid phone number with country code.", groups = ValidationGroups.Format.class)
    private String mobile;

    @Size(max = 100, message = "Address cannot exceed 100 characters.", groups = ValidationGroups.Format.class)
    private String address;

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

}

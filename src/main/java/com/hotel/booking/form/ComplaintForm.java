package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class ComplaintForm {

    @NotNull(message = "Please fill in all required fields.", groups = ValidationGroups.Required.class)
    private ComplaintCategory category;

    private Long bookingId;

    @NotBlank(message = "Please fill in all required fields.", groups = ValidationGroups.Required.class)
    @Size(min = 10, message = "Please provide more details to help us resolve your issue. (Title: at least 10 characters.)", groups = ValidationGroups.Format.class)
    @Size(max = 100, message = "Title cannot exceed 100 characters.", groups = ValidationGroups.Format.class)
    private String title;

    @NotBlank(message = "Please fill in all required fields.", groups = ValidationGroups.Required.class)
    @Size(min = 20, message = "Please provide more details to help us resolve your issue. (Description: at least 20 characters.)", groups = ValidationGroups.Format.class)
    @Size(max = 500, message = "Description cannot exceed 500 characters.", groups = ValidationGroups.Format.class)
    private String description;

    @NotNull(message = "Please fill in all required fields.", groups = ValidationGroups.Required.class)
    private ContactPreference contactPreference;

    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ContactPreference getContactPreference() { return contactPreference; }
    public void setContactPreference(ContactPreference contactPreference) { this.contactPreference = contactPreference; }

}

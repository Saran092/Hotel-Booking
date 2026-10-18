package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class ComplaintActionForm {

    @NotNull(message = "Select the type of entry.")
    private ActionType type = ActionType.STEP;

    @NotBlank(message = "Please enter the details.")
    @Size(max = 1000, message = "Details cannot exceed 1000 characters.")
    private String details;

    private ComplaintStatus newStatus;

    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public ComplaintStatus getNewStatus() { return newStatus; }
    public void setNewStatus(ComplaintStatus newStatus) { this.newStatus = newStatus; }

}

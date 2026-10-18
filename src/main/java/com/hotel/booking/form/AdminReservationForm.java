package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class AdminReservationForm {

    private String customerUsername;

    private Long roomId;

    private LocalDate checkIn;

    private LocalDate checkOut;

    private Integer adults = 1;

    private Integer children = 0;

    @Size(max = 500, message = "Notes cannot exceed 500 characters.")
    private String notes;

    private BookingStatus status;

    public String getCustomerUsername() { return customerUsername; }
    public void setCustomerUsername(String customerUsername) { this.customerUsername = customerUsername; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public Integer getAdults() { return adults; }
    public void setAdults(Integer adults) { this.adults = adults; }

    public Integer getChildren() { return children; }
    public void setChildren(Integer children) { this.children = children; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

}

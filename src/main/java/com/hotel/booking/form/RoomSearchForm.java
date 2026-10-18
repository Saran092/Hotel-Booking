package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class RoomSearchForm {

    private LocalDate checkIn;

    private LocalDate checkOut;

    private Integer adults = 1;

    private Integer children = 0;

    private RoomType roomType;

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public Integer getAdults() { return adults; }
    public void setAdults(Integer adults) { this.adults = adults; }

    public Integer getChildren() { return children; }
    public void setChildren(Integer children) { this.children = children; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public boolean isEmpty() { return checkIn == null && checkOut == null && roomType == null; }

}

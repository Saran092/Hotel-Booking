package com.hotel.booking.service;

import com.hotel.booking.model.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ModificationDraft implements Serializable {

    private Long bookingId;

    private Long roomId;

    private String roomNumber;

    private RoomType roomType;

    private LocalDate checkIn;

    private LocalDate checkOut;

    private int adults;

    private int children;

    private int nights;

    private BigDecimal nightlyRate;

    private BigDecimal base;

    private BigDecimal gst;

    private BigDecimal service;

    private BigDecimal total;

    private BigDecimal difference;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public int getAdults() { return adults; }
    public void setAdults(int adults) { this.adults = adults; }

    public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }

    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }

    public BigDecimal getNightlyRate() { return nightlyRate; }
    public void setNightlyRate(BigDecimal nightlyRate) { this.nightlyRate = nightlyRate; }

    public BigDecimal getBase() { return base; }
    public void setBase(BigDecimal base) { this.base = base; }

    public BigDecimal getGst() { return gst; }
    public void setGst(BigDecimal gst) { this.gst = gst; }

    public BigDecimal getService() { return service; }
    public void setService(BigDecimal service) { this.service = service; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }

    private static final long serialVersionUID = 1L;

}

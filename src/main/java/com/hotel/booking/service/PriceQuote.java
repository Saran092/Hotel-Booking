package com.hotel.booking.service;

import com.hotel.booking.model.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PriceQuote {

    private Room room;

    private LocalDate checkIn;

    private LocalDate checkOut;

    private int nights;

    private BigDecimal nightlyRate;

    private BigDecimal nightlyRateWithTax;

    private BigDecimal base;

    private BigDecimal gst;

    private BigDecimal service;

    private BigDecimal total;

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }

    public BigDecimal getNightlyRate() { return nightlyRate; }
    public void setNightlyRate(BigDecimal nightlyRate) { this.nightlyRate = nightlyRate; }

    public BigDecimal getNightlyRateWithTax() { return nightlyRateWithTax; }
    public void setNightlyRateWithTax(BigDecimal nightlyRateWithTax) { this.nightlyRateWithTax = nightlyRateWithTax; }

    public BigDecimal getBase() { return base; }
    public void setBase(BigDecimal base) { this.base = base; }

    public BigDecimal getGst() { return gst; }
    public void setGst(BigDecimal gst) { this.gst = gst; }

    public BigDecimal getService() { return service; }
    public void setService(BigDecimal service) { this.service = service; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

}

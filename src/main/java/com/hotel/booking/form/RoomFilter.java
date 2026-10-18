package com.hotel.booking.form;

import com.hotel.booking.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class RoomFilter {

    private String q;

    private RoomType roomType;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String availability;

    private String amenity;

    private Integer maxOccupancy;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getAmenity() { return amenity; }
    public void setAmenity(String amenity) { this.amenity = amenity; }

    public Integer getMaxOccupancy() { return maxOccupancy; }
    public void setMaxOccupancy(Integer maxOccupancy) { this.maxOccupancy = maxOccupancy; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

}

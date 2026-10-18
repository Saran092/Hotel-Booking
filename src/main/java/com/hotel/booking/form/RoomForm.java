package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class RoomForm {

    private String roomNumber;

    @NotNull(message = "Select a room type.")
    private RoomType roomType;

    @Size(max = 30, message = "Bed type cannot exceed 30 characters.")
    private String bedType;

    @NotNull(message = "Price per night is required and must be a positive number.")
    @DecimalMin(value = "0.01", message = "Price per night must be a positive number.")
    @Digits(integer = 8, fraction = 2, message = "Enter a valid price (max 2 decimals).")
    private BigDecimal pricePerNight;

    private List<String> amenities = new ArrayList<>();

    @NotNull(message = "Max occupancy is required and must be a positive number.")
    @Min(value = 1, message = "Max occupancy must be a positive number.")
    @Max(value = 10, message = "Max occupancy cannot exceed 10.")
    private Integer maxOccupancy;

    @NotNull(message = "Select the availability.")
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Size(max = 500, message = "Description cannot exceed 500 characters.")
    private String description;

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public Integer getMaxOccupancy() { return maxOccupancy; }
    public void setMaxOccupancy(Integer maxOccupancy) { this.maxOccupancy = maxOccupancy; }

    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

}

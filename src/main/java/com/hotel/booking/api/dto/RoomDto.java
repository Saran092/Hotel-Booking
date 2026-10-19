package com.hotel.booking.api.dto;

import com.hotel.booking.model.Room;

import java.math.BigDecimal;
import java.util.List;

public record RoomDto(Long id, String roomNumber, String roomType, String bedType, BigDecimal pricePerNight,
                      int maxOccupancy, String status, List<String> amenities, String description) {

    public static RoomDto of(Room r) {
        return new RoomDto(r.getId(), r.getRoomNumber(), r.getRoomType().name(), r.getBedType(),
                r.getPricePerNight(), r.getMaxOccupancy(), r.getStatus().name(),
                List.copyOf(r.getAmenities()), r.getDescription());
    }
}

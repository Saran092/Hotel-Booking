package com.hotel.booking.api.dto;

public record LoginResponse(String token, UserSummary user) {
}

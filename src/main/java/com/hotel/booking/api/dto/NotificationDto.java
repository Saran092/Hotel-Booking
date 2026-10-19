package com.hotel.booking.api.dto;

import com.hotel.booking.model.Notification;

import java.time.LocalDateTime;

public record NotificationDto(Long id, String message, boolean read, LocalDateTime createdAt) {

    public static NotificationDto of(Notification n) {
        return new NotificationDto(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}

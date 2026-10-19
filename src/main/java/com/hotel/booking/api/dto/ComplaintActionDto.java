package com.hotel.booking.api.dto;

import com.hotel.booking.model.ComplaintAction;

import java.time.LocalDateTime;

public record ComplaintActionDto(Long id, String actor, String type, String details,
                                 LocalDateTime createdAt, boolean visibleToCustomer) {

    public static ComplaintActionDto of(ComplaintAction a) {
        return new ComplaintActionDto(a.getId(),
                a.getActor() == null ? null : a.getActor().getFullName(),
                a.getType().name(), a.getDetails(), a.getCreatedAt(), a.isVisibleToCustomer());
    }
}

package com.hotel.booking.api.dto;

import com.hotel.booking.model.Complaint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record ComplaintDto(Long id, String reference, UserSummary customer, Long bookingId, String bookingRef,
                           String category, String title, String description, String contactPreference,
                           String status, String priority, LocalDateTime submittedAt, LocalDate expectedResolutionDate,
                           UserSummary assignedTo, String supportResponse, String resolutionNotes,
                           List<ComplaintActionDto> actions) {

    public static ComplaintDto of(Complaint c) {
        return of(c, false);
    }

    /** @param forCustomer customers only see the action history entries that are visible to them. */
    public static ComplaintDto of(Complaint c, boolean forCustomer) {
        return new ComplaintDto(c.getId(), c.getReference(), UserSummary.brief(c.getCustomer()),
                c.getBooking() == null ? null : c.getBooking().getId(),
                c.getBooking() == null ? null : c.getBooking().getBookingRef(),
                c.getCategory().name(), c.getTitle(), c.getDescription(),
                c.getContactPreference() == null ? null : c.getContactPreference().name(),
                c.getStatus().name(), c.getPriority().name(), c.getSubmittedAt(), c.getExpectedResolutionDate(),
                UserSummary.brief(c.getAssignedTo()), c.getSupportResponse(), c.getResolutionNotes(),
                c.getActions().stream()
                        .filter(a -> !forCustomer || a.isVisibleToCustomer())
                        .map(ComplaintActionDto::of).collect(Collectors.toList()));
    }
}

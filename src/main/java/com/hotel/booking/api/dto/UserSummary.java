package com.hotel.booking.api.dto;

import com.hotel.booking.model.Role;
import com.hotel.booking.model.StaffExpertise;
import com.hotel.booking.model.User;

import java.time.LocalDateTime;
import java.util.List;

public record UserSummary(Long id, String username, String fullName, String email, String countryCode,
                          String mobile, String address, String primaryRole, List<String> roles,
                          String expertise, boolean active, boolean mustChangePassword, LocalDateTime lockedUntil) {

    public static UserSummary of(User u) {
        return new UserSummary(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getCountryCode(),
                u.getMobile(), u.getAddress(), u.getPrimaryRole().name(),
                u.getRoles().stream().map(Role::name).sorted().toList(),
                u.getExpertise() == null ? null : u.getExpertise().name(),
                u.isActive(), u.isMustChangePassword(), u.getLockedUntil());
    }

    public static UserSummary brief(User u) {
        return new UserSummary(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getCountryCode(),
                u.getMobile(), null, u.getPrimaryRole().name(),
                u.getRoles().stream().map(Role::name).sorted().toList(),
                u.getExpertise() == null ? null : u.getExpertise().name(),
                u.isActive(), u.isMustChangePassword(), u.getLockedUntil());
    }
}

package com.hotel.booking.controller;

import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.model.*;
import com.hotel.booking.repository.UserRepository;
import com.hotel.booking.service.BillService;
import com.hotel.booking.service.NotificationService;
import com.hotel.booking.service.PaymentService;
import com.hotel.booking.service.RoomService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

/** Values every page needs: the logged-in user, the hotel details and the drop-down lists. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository users;
    private final NotificationService notifications;
    private final HotelProperties props;

    public GlobalModelAdvice(UserRepository users, NotificationService notifications, HotelProperties props) {
        this.users = users;
        this.notifications = notifications;
        this.props = props;
    }

    @ModelAttribute("hotel")
    public HotelProperties hotel() { return props; }

    @ModelAttribute("me")
    public User me(@AuthenticationPrincipal AppUserDetails principal) {
        return principal == null ? null : users.findById(principal.getId()).orElse(null);
    }

    @ModelAttribute("unreadCount")
    public long unread(@AuthenticationPrincipal AppUserDetails principal) {
        if (principal == null) return 0;
        return users.findById(principal.getId()).map(notifications::unreadCount).orElse(0L);
    }

    @ModelAttribute("rx")
    public Map<String, String> patterns() {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("NAME", com.hotel.booking.util.Patterns.NAME);
        m.put("PROFILE_NAME", com.hotel.booking.util.Patterns.PROFILE_NAME);
        m.put("EMAIL", com.hotel.booking.util.Patterns.EMAIL);
        m.put("MOBILE", com.hotel.booking.util.Patterns.MOBILE);
        m.put("USERNAME", com.hotel.booking.util.Patterns.USERNAME);
        m.put("PASSWORD", com.hotel.booking.util.Patterns.PASSWORD);
        m.put("CARD_HOLDER", com.hotel.booking.util.Patterns.CARD_HOLDER);
        m.put("CARD_NUMBER", com.hotel.booking.util.Patterns.CARD_NUMBER);
        m.put("CVV", com.hotel.booking.util.Patterns.CVV);
        m.put("EXPIRY", com.hotel.booking.util.Patterns.EXPIRY);
        m.put("UPI", com.hotel.booking.util.Patterns.UPI);
        m.put("MSG_NAME", com.hotel.booking.util.Patterns.MSG_NAME);
        m.put("MSG_EMAIL", com.hotel.booking.util.Patterns.MSG_EMAIL);
        m.put("MSG_MOBILE", com.hotel.booking.util.Patterns.MSG_MOBILE);
        m.put("MSG_ADDRESS", com.hotel.booking.util.Patterns.MSG_ADDRESS);
        m.put("MSG_USERNAME", com.hotel.booking.util.Patterns.MSG_USERNAME);
        m.put("MSG_PASSWORD", com.hotel.booking.util.Patterns.MSG_PASSWORD);
        return m;
    }

    @ModelAttribute("roomTypes") public RoomType[] roomTypes() { return RoomType.values(); }
    @ModelAttribute("roomStatuses") public RoomStatus[] roomStatuses() { return RoomStatus.values(); }
    @ModelAttribute("bookingStatuses") public BookingStatus[] bookingStatuses() { return BookingStatus.values(); }
    @ModelAttribute("paymentStatuses") public PaymentStatus[] paymentStatuses() { return PaymentStatus.values(); }
    @ModelAttribute("paymentMethods") public PaymentMethod[] paymentMethods() { return PaymentMethod.values(); }
    @ModelAttribute("complaintCategories") public ComplaintCategory[] complaintCategories() { return ComplaintCategory.values(); }
    @ModelAttribute("complaintStatuses") public ComplaintStatus[] complaintStatuses() { return ComplaintStatus.values(); }
    @ModelAttribute("complaintPriorities") public ComplaintPriority[] complaintPriorities() { return ComplaintPriority.values(); }
    @ModelAttribute("contactPreferences") public ContactPreference[] contactPreferences() { return ContactPreference.values(); }
    @ModelAttribute("actionTypes") public ActionType[] actionTypes() { return ActionType.values(); }
    @ModelAttribute("allRoles") public Role[] allRoles() { return Role.values(); }
    @ModelAttribute("expertises") public StaffExpertise[] expertises() { return StaffExpertise.values(); }
    @ModelAttribute("billCategories") public BillItemCategory[] billCategories() { return BillItemCategory.values(); }
    @ModelAttribute("amenityList") public List<String> amenityList() { return RoomService.AMENITIES; }
    @ModelAttribute("bedTypes") public List<String> bedTypes() { return RoomService.BED_TYPES; }
    @ModelAttribute("serviceList") public List<String> serviceList() { return BillService.SERVICES; }
    @ModelAttribute("wallets") public List<String> wallets() { return PaymentService.WALLETS; }
    @ModelAttribute("banks") public List<String> banks() { return PaymentService.BANKS; }
    @ModelAttribute("countryCodes") public Map<String, String> countryCodes() { return Lookups.COUNTRY_CODES; }
}

package com.hotel.booking.api;

import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.controller.Lookups;
import com.hotel.booking.model.*;
import com.hotel.booking.service.BillService;
import com.hotel.booking.service.PaymentService;
import com.hotel.booking.service.RoomService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handy enums, drop-downs and settings needed by the Angular frontend. */
@RestController
@RequestMapping("/api/meta")
public class MetaApi {

    private final HotelProperties props;

    public MetaApi(HotelProperties props) {
        this.props = props;
    }

    @GetMapping
    public Map<String, Object> all() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hotel", hotel());
        m.put("rx", patterns());
        m.put("enums", enums());
        m.put("lists", lists());
        return m;
    }

    private Map<String, Object> hotel() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", props.getName());
        m.put("address", props.getAddress());
        m.put("email", props.getEmail());
        m.put("phone", props.getPhone());
        m.put("gstPercent", props.getGstPercent());
        m.put("serviceChargePercent", props.getServiceChargePercent());
        m.put("maxFailedLogins", props.getMaxFailedLogins());
        m.put("lockMinutes", props.getLockMinutes());
        m.put("modificationCutoffHours", props.getModificationCutoffHours());
        m.put("freeCancellationHours", props.getFreeCancellationHours());
        m.put("partialRefundHours", props.getPartialRefundHours());
        m.put("partialRefundPercent", props.getPartialRefundPercent());
        m.put("paymentSessionMinutes", props.getPaymentSessionMinutes());
        m.put("complaintSlaDays", props.getComplaintSlaDays());
        return m;
    }

    private Map<String, String> patterns() {
        Map<String, String> m = new LinkedHashMap<>();
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

    private Map<String, Object> enums() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roomTypes", RoomType.values());
        m.put("roomStatuses", RoomStatus.values());
        m.put("bookingStatuses", BookingStatus.values());
        m.put("paymentStatuses", PaymentStatus.values());
        m.put("paymentMethods", PaymentMethod.values());
        m.put("complaintCategories", ComplaintCategory.values());
        m.put("complaintStatuses", ComplaintStatus.values());
        m.put("complaintPriorities", ComplaintPriority.values());
        m.put("contactPreferences", ContactPreference.values());
        m.put("actionTypes", ActionType.values());
        m.put("allRoles", Role.values());
        m.put("expertises", StaffExpertise.values());
        m.put("billCategories", BillItemCategory.values());
        m.put("countryCodes", Lookups.COUNTRY_CODES);
        return m;
    }

    private Map<String, Object> lists() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("amenityList", RoomService.AMENITIES);
        m.put("bedTypes", RoomService.BED_TYPES);
        m.put("serviceList", BillService.SERVICES);
        m.put("wallets", PaymentService.WALLETS);
        m.put("banks", PaymentService.BANKS);
        return m;
    }
}

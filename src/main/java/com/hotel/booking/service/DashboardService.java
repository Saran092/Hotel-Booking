package com.hotel.booking.service;

import com.hotel.booking.model.BookingStatus;
import com.hotel.booking.model.ComplaintStatus;
import com.hotel.booking.model.PaymentStatus;
import com.hotel.booking.repository.BillRepository;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.ComplaintRepository;
import com.hotel.booking.repository.RoomRepository;
import org.springframework.stereotype.Service;

import com.hotel.booking.model.Booking;
import com.hotel.booking.model.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Numbers for the admin home page. */
@Service
public class DashboardService {

    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final ComplaintRepository complaints;
    private final BillRepository bills;
    private final RoomService roomService;

    public DashboardService(BookingRepository bookings, RoomRepository rooms, ComplaintRepository complaints,
                            BillRepository bills, RoomService roomService) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.complaints = complaints;
        this.bills = bills;
        this.roomService = roomService;
    }

    public Map<String, Object> adminStats() {
        LocalDate today = LocalDate.now();
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("today", bookings.countByCreatedAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        s.put("week", bookings.countByCreatedAtBetween(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()));
        s.put("month", bookings.countByCreatedAtBetween(today.withDayOfMonth(1).atStartOfDay(), today.plusDays(1).atStartOfDay()));
        long totalRooms = rooms.count();
        long available = rooms.findAll().stream().filter(roomService::isAvailableNow).count();
        s.put("totalRooms", totalRooms);
        s.put("availableRooms", available);

        // last 7 days, oldest first, for the bar chart
        Map<String, Long> daily = new LinkedHashMap<>();
        long max = 1;
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long n = bookings.countByCreatedAtBetween(d.atStartOfDay(), d.plusDays(1).atStartOfDay());
            daily.put(d.format(DateTimeFormatter.ofPattern("dd MMM")), n);
            max = Math.max(max, n);
        }
        s.put("daily", daily);
        s.put("maxDaily", max);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (BookingStatus st : BookingStatus.values()) {
            byStatus.put(st.getLabel(), bookings.findAll().stream().filter(b -> b.getEffectiveStatus() == st).count());
        }
        s.put("byStatus", byStatus);
        s.put("maxStatus", byStatus.values().stream().mapToLong(Long::longValue).max().orElse(1L) == 0 ? 1L
                : byStatus.values().stream().mapToLong(Long::longValue).max().orElse(1L));

        s.put("openComplaints", complaints.findAll().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.OPEN || c.getStatus() == ComplaintStatus.ESCALATED).count());
        s.put("pendingBills", bills.findAll().stream().filter(b -> b.getPaymentStatus() == PaymentStatus.PENDING).count());
        return s;
    }

    /** Extra figures for the "Reports and Analytics" page. */
    public Map<String, Object> reports() {
        Map<String, Object> r = new LinkedHashMap<>(adminStats());
        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        for (RoomType t : RoomType.values()) revenue.put(t.getLabel(), BigDecimal.ZERO);
        for (Booking b : bookings.findAll()) {
            BigDecimal net = b.getStatus() == BookingStatus.CANCELLED ? BigDecimal.ZERO : b.getAmountPaid();
            revenue.merge(b.getRoom().getRoomType().getLabel(), net, BigDecimal::add);
        }
        r.put("revenue", revenue);
        r.put("maxRevenue", revenue.values().stream().max(BigDecimal::compareTo).filter(v -> v.signum() > 0).orElse(BigDecimal.ONE));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ComplaintStatus st : ComplaintStatus.values()) {
            byStatus.put(st.getLabel(), complaints.findAll().stream().filter(c -> c.getStatus() == st).count());
        }
        r.put("complaintsByStatus", byStatus);
        long maxC = byStatus.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        r.put("maxComplaints", Math.max(1L, maxC));
        return r;
    }
}

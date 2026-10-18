package com.hotel.booking.service;

import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.form.BillFilter;
import com.hotel.booking.form.BillForm;
import com.hotel.booking.form.BillItemForm;
import com.hotel.booking.model.*;
import com.hotel.booking.repository.BillRepository;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class BillService {

    public static final List<String> SERVICES = List.of("Spa Treatment", "Dining", "Room Service", "Laundry", "Late Check-out",
            "Mini-Bar", "Airport Transfer");

    private final BillRepository bills;
    private final UserRepository users;
    private final BookingRepository bookings;
    private final HotelProperties props;

    public BillService(BillRepository bills, UserRepository users, BookingRepository bookings, HotelProperties props) {
        this.bills = bills;
        this.users = users;
        this.bookings = bookings;
        this.props = props;
    }

    public Bill get(Long id) {
        return bills.findById(id).orElseThrow(() -> new BusinessException("Bill not found."));
    }

    // ------------------------------------------------------------------ bill for a booking (kept in sync automatically)

    /** Creates or refreshes the bill that mirrors the room charges of a booking. */
    @Transactional
    public Bill syncBookingBill(Booking b) {
        Bill bill = bills.findByBooking(b).stream().findFirst().orElseGet(() -> {
            Bill n = new Bill();
            n.setCustomer(b.getCustomer());
            n.setBooking(b);
            return n;
        });
        bill.getItems().clear();

        BillItem room = new BillItem();
        room.setCategory(BillItemCategory.ROOM);
        room.setDescription("Room " + b.getRoom().getRoomNumber() + " (" + b.getRoom().getRoomType().getLabel() + "), "
                + b.getNights() + " night(s) - booking " + b.getBookingRef());
        room.setQuantity(b.getNights());
        room.setUnitPrice(b.getNightlyRate());
        room.setTaxPercent(props.getGstPercent());
        bill.addItem(room);

        BillItem service = new BillItem();
        service.setCategory(BillItemCategory.SERVICE);
        service.setDescription("Service charge (" + props.getServiceChargePercent().stripTrailingZeros().toPlainString() + "%)");
        service.setServiceDateTime(b.getCreatedAt());
        service.setQuantity(1);
        service.setUnitPrice(b.getServiceChargeAmount());
        bill.addItem(service);

        bill.setPaymentStatus(b.getPaymentStatus());
        bill.recalculateTotal();
        return bills.save(bill);
    }

    // ------------------------------------------------------------------ admin list / search

    public List<Bill> filter(BillFilter f) {
        String customer = f.getCustomer() == null ? "" : f.getCustomer().trim().toLowerCase(Locale.ROOT);
        String billId = f.getBillId() == null ? "" : f.getBillId().trim().toLowerCase(Locale.ROOT);
        return bills.findAllByOrderByIssueDateDesc().stream()
                .filter(b -> billId.isEmpty() || b.getBillRef().toLowerCase(Locale.ROOT).contains(billId)
                        || String.valueOf(b.getId()).equals(billId))
                .filter(b -> customer.isEmpty() || b.getCustomer().getFullName().toLowerCase(Locale.ROOT).contains(customer)
                        || b.getCustomer().getUsername().toLowerCase(Locale.ROOT).contains(customer)
                        || String.valueOf(b.getCustomer().getId()).equals(customer))
                .filter(b -> f.getDateFrom() == null || !b.getIssueDate().toLocalDate().isBefore(f.getDateFrom()))
                .filter(b -> f.getDateTo() == null || !b.getIssueDate().toLocalDate().isAfter(f.getDateTo()))
                .filter(b -> f.getStatus() == null || b.getPaymentStatus() == f.getStatus())
                .filter(b -> f.getMinAmount() == null || b.getTotalAmount().compareTo(f.getMinAmount()) >= 0)
                .filter(b -> f.getMaxAmount() == null || b.getTotalAmount().compareTo(f.getMaxAmount()) <= 0)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ add / edit

    public BillForm toForm(Bill bill) {
        BillForm f = new BillForm();
        f.setCustomerId(bill.getCustomer().getId());
        f.setBookingId(bill.getBooking() == null ? null : bill.getBooking().getId());
        f.setPaymentStatus(bill.getPaymentStatus());
        f.setNotes(bill.getNotes());
        for (BillItem i : bill.getItems()) {
            BillItemForm x = new BillItemForm();
            x.setCategory(i.getCategory());
            x.setDescription(i.getDescription());
            x.setServiceDateTime(i.getServiceDateTime());
            x.setQuantity(i.getQuantity());
            x.setUnitPrice(i.getUnitPrice());
            x.setTaxPercent(i.getTaxPercent());
            x.setDiscount(i.getDiscount());
            f.getItems().add(x);
        }
        return f;
    }

    public void validate(BillForm f, BindingResult errors) {
        if (f.getCustomerId() == null || users.findById(f.getCustomerId()).isEmpty()) {
            errors.rejectValue("customerId", "required", "Select a customer.");
        }
        if (f.getPaymentStatus() == null) {
            errors.rejectValue("paymentStatus", "required", "Select the payment status.");
        }
        List<BillItemForm> items = f.getItems() == null ? new ArrayList<>() : f.getItems();
        if (items.isEmpty()) {
            errors.rejectValue("items", "required", "Add at least one charge to the bill.");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            BillItemForm it = items.get(i);
            String p = "items[" + i + "].";
            if (it.getCategory() == null) errors.rejectValue(p + "category", "required", "Select a category.");
            if (!StringUtils.hasText(it.getDescription())) errors.rejectValue(p + "description", "required", "Description is required.");
            else if (it.getDescription().length() > 200) errors.rejectValue(p + "description", "size", "Description cannot exceed 200 characters.");
            if (it.getCategory() == BillItemCategory.SERVICE && it.getServiceDateTime() == null) {
                errors.rejectValue(p + "serviceDateTime", "required", "Service date and time is required.");
            }
            if (it.getQuantity() == null || it.getQuantity() < 1) errors.rejectValue(p + "quantity", "min", "Quantity must be at least 1.");
            if (it.getUnitPrice() == null || it.getUnitPrice().signum() < 0) errors.rejectValue(p + "unitPrice", "min", "Unit price must be 0 or more.");
            if (it.getTaxPercent() == null || it.getTaxPercent().signum() < 0 || it.getTaxPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
                errors.rejectValue(p + "taxPercent", "range", "Tax must be between 0 and 100%.");
            }
            if (it.getDiscount() == null || it.getDiscount().signum() < 0) errors.rejectValue(p + "discount", "min", "Discount must be 0 or more.");
            if (!errors.hasErrors()) total = total.add(toItem(it).getLineTotal());
        }
        if (!errors.hasErrors() && total.signum() < 0) {
            errors.reject("negative", "The total amount due cannot be negative. Check the discounts.");
        }
    }

    private BillItem toItem(BillItemForm f) {
        BillItem i = new BillItem();
        i.setCategory(f.getCategory());
        i.setDescription(f.getDescription() == null ? "" : f.getDescription().trim());
        i.setServiceDateTime(f.getServiceDateTime());
        i.setQuantity(f.getQuantity());
        i.setUnitPrice(f.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
        i.setTaxPercent(f.getTaxPercent());
        i.setDiscount(f.getDiscount().setScale(2, RoundingMode.HALF_UP));
        return i;
    }

    @Transactional
    public Bill save(Long id, BillForm f) {
        Bill bill = id == null ? new Bill() : get(id);
        bill.setCustomer(users.findById(f.getCustomerId()).orElseThrow(() -> new BusinessException("Customer not found.")));
        bill.setBooking(f.getBookingId() == null ? null : bookings.findById(f.getBookingId()).orElse(null));
        bill.setPaymentStatus(f.getPaymentStatus());
        bill.setNotes(StringUtils.hasText(f.getNotes()) ? f.getNotes().trim() : null);
        if (id == null) bill.setIssueDate(LocalDateTime.now());
        bill.getItems().clear();
        f.getItems().forEach(i -> bill.addItem(toItem(i)));
        bill.recalculateTotal();
        return bills.save(bill);
    }
}

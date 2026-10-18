package com.hotel.booking.service;

import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.form.ComplaintActionForm;
import com.hotel.booking.form.ComplaintFilter;
import com.hotel.booking.form.ComplaintForm;
import com.hotel.booking.model.*;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.ComplaintRepository;
import com.hotel.booking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<ComplaintStatus> ACTIVE = List.of(ComplaintStatus.OPEN, ComplaintStatus.IN_PROGRESS, ComplaintStatus.ESCALATED);

    /** Which kind of staff handles which kind of complaint. */
    private static final Map<ComplaintCategory, StaffExpertise> EXPERTISE = Map.of(
            ComplaintCategory.BILLING_ISSUE, StaffExpertise.BILLING,
            ComplaintCategory.ROOM_ISSUE, StaffExpertise.MAINTENANCE,
            ComplaintCategory.SERVICE_ISSUE, StaffExpertise.HOUSEKEEPING,
            ComplaintCategory.OTHER, StaffExpertise.GENERAL);

    private final ComplaintRepository complaints;
    private final UserRepository users;
    private final BookingRepository bookings;
    private final NotificationService notifications;
    private final HotelProperties props;

    public ComplaintService(ComplaintRepository complaints, UserRepository users, BookingRepository bookings,
                            NotificationService notifications, HotelProperties props) {
        this.complaints = complaints;
        this.users = users;
        this.bookings = bookings;
        this.notifications = notifications;
        this.props = props;
    }

    private static final String NOT_FOUND = "The complaint you are looking for does not exist or has been deleted.";

    public Complaint get(Long id) {
        return complaints.findById(id).orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    public Complaint getForCustomer(Long id, User customer) {
        Complaint c = get(id);
        if (!c.getCustomer().getId().equals(customer.getId())) throw new BusinessException(NOT_FOUND);
        return c;
    }

    /** Staff only see complaints assigned to them; admins see everything. */
    public Complaint getForStaff(Long id, User staff) {
        Complaint c = get(id);
        boolean admin = staff.hasRole(Role.ADMIN);
        if (!admin && (c.getAssignedTo() == null || !c.getAssignedTo().getId().equals(staff.getId()))) {
            throw new BusinessException(NOT_FOUND);
        }
        return c;
    }

    public List<Complaint> forCustomer(User customer) {
        return complaints.findByCustomerOrderBySubmittedAtDesc(customer);
    }

    // ------------------------------------------------------------------ customer side (US011 / US012)

    public void validateBooking(ComplaintForm f, User customer, BindingResult errors) {
        if (f.getBookingId() != null) {
            boolean mine = bookings.findById(f.getBookingId()).map(b -> b.getCustomer().getId().equals(customer.getId())).orElse(false);
            if (!mine) errors.rejectValue("bookingId", "invalid", "Select one of your own bookings.");
        }
    }

    @Transactional
    public Complaint submit(User customer, ComplaintForm f) {
        Complaint c = new Complaint();
        c.setReference(newReference());
        c.setCustomer(customer);
        c.setBooking(f.getBookingId() == null ? null : bookings.findById(f.getBookingId()).orElse(null));
        c.setCategory(f.getCategory());
        c.setTitle(f.getTitle().trim());
        c.setDescription(f.getDescription().trim());
        c.setContactPreference(f.getContactPreference());
        c.setPriority(switch (f.getCategory()) {
            case ROOM_ISSUE -> ComplaintPriority.HIGH;
            case OTHER -> ComplaintPriority.LOW;
            default -> ComplaintPriority.MEDIUM;
        });
        c.setExpectedResolutionDate(LocalDate.now().plusDays(props.getComplaintSlaDays()));
        c.setAssignedTo(pickStaff(f.getCategory()));
        complaints.save(c);

        notifications.notify(customer, "Your complaint has been successfully submitted. Complaint ID: " + c.getReference()
                + ". Expected resolution by " + c.getExpectedResolutionDate() + ". Our support team will get back to you soon.");
        if (c.getAssignedTo() != null) {
            notifications.notify(c.getAssignedTo(), "New complaint " + c.getReference() + " (" + c.getCategory().getLabel() + ") has been assigned to you.");
        }
        return c;
    }

    private String newReference() {
        String ref;
        do {
            ref = "CMP" + (100000 + RANDOM.nextInt(900000));
        } while (complaints.findByReference(ref).isPresent());
        return ref;
    }

    /** Least-busy staff member with the matching expertise (or a general one). */
    private User pickStaff(ComplaintCategory category) {
        StaffExpertise wanted = EXPERTISE.get(category);
        List<User> staff = users.findByRole(Role.STAFF).stream().filter(User::isActive).collect(Collectors.toList());
        return staff.stream().filter(u -> u.getExpertise() == wanted)
                .min(Comparator.comparingLong(u -> complaints.countByAssignedToAndStatusIn(u, ACTIVE)))
                .or(() -> staff.stream().filter(u -> u.getExpertise() == StaffExpertise.GENERAL)
                        .min(Comparator.comparingLong(u -> complaints.countByAssignedToAndStatusIn(u, ACTIVE))))
                .orElse(null);
    }

    public ComplaintForm toForm(Complaint c) {
        ComplaintForm f = new ComplaintForm();
        f.setCategory(c.getCategory());
        f.setBookingId(c.getBooking() == null ? null : c.getBooking().getId());
        f.setTitle(c.getTitle());
        f.setDescription(c.getDescription());
        f.setContactPreference(c.getContactPreference());
        return f;
    }

    /** Customers can edit a complaint only while it is still Open. */
    @Transactional
    public Complaint updateByCustomer(Long id, User customer, ComplaintForm f) {
        Complaint c = getForCustomer(id, customer);
        if (c.getStatus() != ComplaintStatus.OPEN) {
            throw new BusinessException("Only complaints in the 'Open' status can be edited.");
        }
        c.setCategory(f.getCategory());
        c.setBooking(f.getBookingId() == null ? null : bookings.findById(f.getBookingId()).orElse(null));
        c.setTitle(f.getTitle().trim());
        c.setDescription(f.getDescription().trim());
        c.setContactPreference(f.getContactPreference());
        return complaints.save(c);
    }

    @Transactional
    public Complaint confirmResolution(Long id, User customer) {
        Complaint c = getForCustomer(id, customer);
        if (c.getStatus() != ComplaintStatus.RESOLVED) throw new BusinessException("Only a resolved complaint can be confirmed.");
        c.setStatus(ComplaintStatus.CLOSED);
        log(c, customer, ActionType.STATUS_CHANGE, "Customer confirmed the resolution. Complaint closed.", true);
        notifications.notify(c.getAssignedTo(), "Complaint " + c.getReference() + " was confirmed as resolved and closed by the customer.");
        return complaints.save(c);
    }

    @Transactional
    public Complaint reopen(Long id, User customer) {
        Complaint c = getForCustomer(id, customer);
        if (c.getStatus() != ComplaintStatus.RESOLVED) throw new BusinessException("Only a resolved complaint can be reopened.");
        c.setStatus(ComplaintStatus.OPEN);
        c.setExpectedResolutionDate(LocalDate.now().plusDays(props.getComplaintSlaDays()));
        log(c, customer, ActionType.STATUS_CHANGE, "Customer reopened the complaint because the issue is not resolved.", true);
        notifications.notify(c.getAssignedTo(), "Complaint " + c.getReference() + " was reopened by the customer.");
        return complaints.save(c);
    }

    // ------------------------------------------------------------------ admin / staff side (US022 / US023)

    @Transactional
    public Complaint assign(Long id, Long staffId, User admin) {
        Complaint c = get(id);
        if (c.getStatus() == ComplaintStatus.CLOSED) throw new BusinessException("This complaint is closed and cannot be modified.");
        User staff = users.findById(staffId).filter(u -> u.hasRole(Role.STAFF)).orElseThrow(() -> new BusinessException("Select a valid staff member."));
        c.setAssignedTo(staff);
        log(c, admin, ActionType.STATUS_CHANGE, "Assigned to " + staff.getFullName() + " (" + staff.getExpertise().getLabel() + ").", false);
        notifications.notify(staff, "Complaint " + c.getReference() + " (" + c.getCategory().getLabel() + ") has been assigned to you.");
        return complaints.save(c);
    }

    @Transactional
    public Complaint setPriority(Long id, ComplaintPriority priority, User actor) {
        Complaint c = getForStaff(id, actor);
        if (c.getStatus() == ComplaintStatus.CLOSED) throw new BusinessException("This complaint is closed and cannot be modified.");
        c.setPriority(priority);
        return complaints.save(c);
    }

    /** Logs a step / communication / note and optionally moves the complaint to a new status. */
    @Transactional
    public Complaint addAction(Long id, User actor, ComplaintActionForm f) {
        Complaint c = getForStaff(id, actor);
        if (c.getStatus() == ComplaintStatus.CLOSED) throw new BusinessException("This complaint is closed and cannot be modified.");
        boolean admin = actor.hasRole(Role.ADMIN);
        ComplaintStatus target = f.getNewStatus();
        if (f.getType() == ActionType.RESOLUTION && target == null) target = ComplaintStatus.RESOLVED;
        if (target != null && !admin && !EnumSet.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED, ComplaintStatus.ESCALATED).contains(target)) {
            throw new BusinessException("Staff can set the status to In Progress, Resolved or Escalated.");
        }
        if (f.getType() == ActionType.STATUS_CHANGE) {
            throw new BusinessException("Choose what kind of entry you are adding.");
        }
        boolean visible = f.getType() == ActionType.COMMUNICATION || f.getType() == ActionType.RESOLUTION;
        log(c, actor, f.getType(), f.getDetails().trim(), visible);
        if (f.getType() == ActionType.COMMUNICATION) c.setSupportResponse(f.getDetails().trim());
        if (f.getType() == ActionType.RESOLUTION) c.setResolutionNotes(f.getDetails().trim());
        if (visible) {
            notifications.notify(c.getCustomer(), "Update on complaint " + c.getReference() + ": " + f.getDetails().trim());
        }
        if (target != null && target != c.getStatus()) {
            ComplaintStatus old = c.getStatus();
            c.setStatus(target);
            log(c, actor, ActionType.STATUS_CHANGE, "Status changed from " + old.getLabel() + " to " + target.getLabel() + ".", true);
            notifications.notify(c.getCustomer(), "Your complaint " + c.getReference() + " is now '" + target.getLabel() + "'."
                    + (target == ComplaintStatus.RESOLVED ? " Please confirm the resolution or reopen the complaint." : ""));
        }
        return complaints.save(c);
    }

    private void log(Complaint c, User actor, ActionType type, String details, boolean visible) {
        ComplaintAction a = new ComplaintAction();
        a.setActor(actor);
        a.setType(type);
        a.setDetails(details);
        a.setVisibleToCustomer(visible);
        c.addAction(a);
    }

    public List<Complaint> filter(ComplaintFilter f, User staffOnly) {
        String customer = f.getCustomer() == null ? "" : f.getCustomer().trim().toLowerCase(Locale.ROOT);
        return complaints.findAllByOrderBySubmittedAtDesc().stream()
                .filter(c -> staffOnly == null || (c.getAssignedTo() != null && c.getAssignedTo().getId().equals(staffOnly.getId())))
                .filter(c -> f.getDateFrom() == null || !c.getSubmittedAt().toLocalDate().isBefore(f.getDateFrom()))
                .filter(c -> f.getDateTo() == null || !c.getSubmittedAt().toLocalDate().isAfter(f.getDateTo()))
                .filter(c -> f.getCategory() == null || c.getCategory() == f.getCategory())
                .filter(c -> f.getStatus() == null || c.getStatus() == f.getStatus())
                .filter(c -> f.getAssignedTo() == null || (c.getAssignedTo() != null && c.getAssignedTo().getId().equals(f.getAssignedTo())))
                .filter(c -> customer.isEmpty() || c.getCustomer().getFullName().toLowerCase(Locale.ROOT).contains(customer)
                        || c.getCustomer().getUsername().toLowerCase(Locale.ROOT).contains(customer)
                        || String.valueOf(c.getCustomer().getId()).equals(customer)
                        || c.getReference().toLowerCase(Locale.ROOT).contains(customer))
                .collect(Collectors.toList());
    }

    public static boolean hasText(String s) { return StringUtils.hasText(s); }
}

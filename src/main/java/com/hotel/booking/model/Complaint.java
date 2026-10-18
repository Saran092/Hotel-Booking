package com.hotel.booking.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String reference;

    @ManyToOne(optional = false)
    private User customer;

    @ManyToOne
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    private ContactPreference contactPreference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    private LocalDateTime submittedAt = LocalDateTime.now();

    private LocalDate expectedResolutionDate;

    @ManyToOne
    private User assignedTo;

    @Column(length = 1000)
    private String supportResponse;

    @Column(length = 1000)
    private String resolutionNotes;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<ComplaintAction> actions = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ContactPreference getContactPreference() { return contactPreference; }
    public void setContactPreference(ContactPreference contactPreference) { this.contactPreference = contactPreference; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public ComplaintPriority getPriority() { return priority; }
    public void setPriority(ComplaintPriority priority) { this.priority = priority; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDate getExpectedResolutionDate() { return expectedResolutionDate; }
    public void setExpectedResolutionDate(LocalDate expectedResolutionDate) { this.expectedResolutionDate = expectedResolutionDate; }

    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }

    public String getSupportResponse() { return supportResponse; }
    public void setSupportResponse(String supportResponse) { this.supportResponse = supportResponse; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public List<ComplaintAction> getActions() { return actions; }
    public void setActions(List<ComplaintAction> actions) { this.actions = actions; }

    public void addAction(ComplaintAction action) { action.setComplaint(this); actions.add(action); }

}

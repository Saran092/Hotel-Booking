package com.hotel.booking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_actions")
public class ComplaintAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Complaint complaint;

    @ManyToOne
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType type;

    @Column(nullable = false, length = 1000)
    private String details;

    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean visibleToCustomer = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }

    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isVisibleToCustomer() { return visibleToCustomer; }
    public void setVisibleToCustomer(boolean visibleToCustomer) { this.visibleToCustomer = visibleToCustomer; }

}

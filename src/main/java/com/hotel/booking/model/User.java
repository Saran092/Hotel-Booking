package com.hotel.booking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(length = 6)
    private String countryCode;

    @Column(length = 15)
    private String mobile;

    @Column(length = 255)
    private String address;

    @Column(unique = true, nullable = false, length = 60)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    private boolean active = true;

    private boolean mustChangePassword = false;

    private int failedAttempts = 0;

    private LocalDateTime lockedUntil;

    @Enumerated(EnumType.STRING)
    private StaffExpertise expertise;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public StaffExpertise getExpertise() { return expertise; }
    public void setExpertise(StaffExpertise expertise) { this.expertise = expertise; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean hasRole(Role role) { return roles.contains(role); }

    public boolean isLocked() { return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now()); }

    /** Highest-privilege role, used to decide where the user lands after login. */
    public Role getPrimaryRole() {
        if (roles.contains(Role.ADMIN)) return Role.ADMIN;
        if (roles.contains(Role.STAFF)) return Role.STAFF;
        return Role.CUSTOMER;
    }

    public String getFullMobile() {
        return mobile == null ? "" : (countryCode == null ? "" : countryCode + " ") + mobile;
    }

}

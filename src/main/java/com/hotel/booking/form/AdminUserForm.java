package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class AdminUserForm {

    @Pattern(regexp = Patterns.USERNAME, message = Patterns.MSG_USERNAME)
    private String username = "";

    private String fullName;

    @Pattern(regexp = Patterns.EMAIL, message = Patterns.MSG_EMAIL)
    private String email = "";

    @NotEmpty(message = "Assign at least one role.")
    private Set<Role> roles = new HashSet<>();

    private StaffExpertise expertise;

    private boolean active = true;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public StaffExpertise getExpertise() { return expertise; }
    public void setExpertise(StaffExpertise expertise) { this.expertise = expertise; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

}

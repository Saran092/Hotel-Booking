package com.hotel.booking.form;

import com.hotel.booking.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class UserFilter {

    private String q;

    private Role role;

    private String status;

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

}

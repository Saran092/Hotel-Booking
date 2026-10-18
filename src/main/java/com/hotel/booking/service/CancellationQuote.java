package com.hotel.booking.service;

import com.hotel.booking.model.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CancellationQuote {

    private boolean allowed;

    private String blockedReason;

    private int refundPercent;

    private BigDecimal refundAmount = BigDecimal.ZERO;

    private String message;

    public boolean isAllowed() { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }

    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }

    public int getRefundPercent() { return refundPercent; }
    public void setRefundPercent(int refundPercent) { this.refundPercent = refundPercent; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

}

package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class BillItemForm {

    private BillItemCategory category = BillItemCategory.SERVICE;

    private String description;

    private LocalDateTime serviceDateTime;

    private Integer quantity = 1;

    private BigDecimal unitPrice;

    private BigDecimal taxPercent = BigDecimal.ZERO;

    private BigDecimal discount = BigDecimal.ZERO;

    public BillItemCategory getCategory() { return category; }
    public void setCategory(BillItemCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getServiceDateTime() { return serviceDateTime; }
    public void setServiceDateTime(LocalDateTime serviceDateTime) { this.serviceDateTime = serviceDateTime; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTaxPercent() { return taxPercent; }
    public void setTaxPercent(BigDecimal taxPercent) { this.taxPercent = taxPercent; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

}

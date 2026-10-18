package com.hotel.booking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_items")
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillItemCategory category;

    @Column(length = 200)
    private String description;

    private LocalDateTime serviceDateTime;

    private int quantity = 1;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Bill getBill() { return bill; }
    public void setBill(Bill bill) { this.bill = bill; }

    public BillItemCategory getCategory() { return category; }
    public void setCategory(BillItemCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getServiceDateTime() { return serviceDateTime; }
    public void setServiceDateTime(LocalDateTime serviceDateTime) { this.serviceDateTime = serviceDateTime; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTaxPercent() { return taxPercent; }
    public void setTaxPercent(BigDecimal taxPercent) { this.taxPercent = taxPercent; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    /** quantity x unit price, plus tax, minus discount. */
    public BigDecimal getLineTotal() {
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal tax = gross.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return gross.add(tax).subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

}

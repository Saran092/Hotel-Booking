package com.hotel.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Hotel details and business rules, bound from the "hotel.*" keys of application.properties. */
@Component
@ConfigurationProperties(prefix = "hotel")
public class HotelProperties {

    private String name = "Hotel";
    private String address = "";
    private String email = "";
    private String phone = "";
    private BigDecimal gstPercent = new BigDecimal("12");
    private BigDecimal serviceChargePercent = new BigDecimal("5");
    private int maxFailedLogins = 5;
    private int lockMinutes = 15;
    private int modificationCutoffHours = 24;
    private int freeCancellationHours = 48;
    private int partialRefundHours = 24;
    private int partialRefundPercent = 50;
    private int paymentSessionMinutes = 10;
    private int complaintSlaDays = 3;
    private boolean seedDemoData = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public BigDecimal getGstPercent() { return gstPercent; }
    public void setGstPercent(BigDecimal gstPercent) { this.gstPercent = gstPercent; }
    public BigDecimal getServiceChargePercent() { return serviceChargePercent; }
    public void setServiceChargePercent(BigDecimal serviceChargePercent) { this.serviceChargePercent = serviceChargePercent; }
    public int getMaxFailedLogins() { return maxFailedLogins; }
    public void setMaxFailedLogins(int maxFailedLogins) { this.maxFailedLogins = maxFailedLogins; }
    public int getLockMinutes() { return lockMinutes; }
    public void setLockMinutes(int lockMinutes) { this.lockMinutes = lockMinutes; }
    public int getModificationCutoffHours() { return modificationCutoffHours; }
    public void setModificationCutoffHours(int modificationCutoffHours) { this.modificationCutoffHours = modificationCutoffHours; }
    public int getFreeCancellationHours() { return freeCancellationHours; }
    public void setFreeCancellationHours(int freeCancellationHours) { this.freeCancellationHours = freeCancellationHours; }
    public int getPartialRefundHours() { return partialRefundHours; }
    public void setPartialRefundHours(int partialRefundHours) { this.partialRefundHours = partialRefundHours; }
    public int getPartialRefundPercent() { return partialRefundPercent; }
    public void setPartialRefundPercent(int partialRefundPercent) { this.partialRefundPercent = partialRefundPercent; }
    public int getPaymentSessionMinutes() { return paymentSessionMinutes; }
    public void setPaymentSessionMinutes(int paymentSessionMinutes) { this.paymentSessionMinutes = paymentSessionMinutes; }
    public int getComplaintSlaDays() { return complaintSlaDays; }
    public void setComplaintSlaDays(int complaintSlaDays) { this.complaintSlaDays = complaintSlaDays; }
    public boolean isSeedDemoData() { return seedDemoData; }
    public void setSeedDemoData(boolean seedDemoData) { this.seedDemoData = seedDemoData; }
}

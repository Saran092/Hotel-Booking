package com.hotel.booking.form;

import com.hotel.booking.model.*;
import com.hotel.booking.util.Patterns;
import com.hotel.booking.util.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class PaymentForm {

    private String cardHolder = "";

    private String cardNumber = "";

    private String expiry = "";

    private String cvv = "";

    private String billingAddress = "";

    private String upiId = "";

    private String wallet = "";

    private String bank = "";

    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getWallet() { return wallet; }
    public void setWallet(String wallet) { this.wallet = wallet; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

}

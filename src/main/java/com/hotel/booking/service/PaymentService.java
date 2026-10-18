package com.hotel.booking.service;

import com.hotel.booking.form.PaymentForm;
import com.hotel.booking.model.Booking;
import com.hotel.booking.model.Payment;
import com.hotel.booking.model.PaymentMethod;
import com.hotel.booking.repository.PaymentRepository;
import com.hotel.booking.util.Patterns;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates payment details and talks to the payment gateway.
 * The gateway here is a simulator: a card number ending in 0000 (or a UPI id starting with "fail") is declined,
 * everything else succeeds. Replace {@link #callGateway} with a real gateway client for production.
 * The full card number and CVV are never stored - only the last four digits.
 */
@Service
public class PaymentService {

    public static final List<String> WALLETS = List.of("Paytm", "PhonePe", "Amazon Pay", "Google Pay");
    public static final List<String> BANKS = List.of("State Bank of India", "HDFC Bank", "ICICI Bank", "Axis Bank", "Indian Bank");

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentRepository payments;

    public PaymentService(PaymentRepository payments) {
        this.payments = payments;
    }

    public static boolean isCard(PaymentMethod m) {
        return m == PaymentMethod.CREDIT_CARD || m == PaymentMethod.DEBIT_CARD;
    }

    /** @return field name -> message; empty when the details are valid */
    public Map<String, String> validate(PaymentMethod method, PaymentForm f) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (method == null) {
            errors.put("method", "Select a payment method.");
            return errors;
        }
        switch (method) {
            case CREDIT_CARD, DEBIT_CARD -> {
                f.setCardNumber(f.getCardNumber().replaceAll("[\\s-]", ""));
                if (!f.getCardHolder().trim().matches(Patterns.CARD_HOLDER)) {
                    errors.put("cardHolder", "Cardholder name must be 3-50 letters (alphabetic characters only).");
                }
                if (!f.getCardNumber().matches(Patterns.CARD_NUMBER)) {
                    errors.put("cardNumber", "Invalid card number.");
                }
                if (!f.getExpiry().trim().matches(Patterns.EXPIRY)) {
                    errors.put("expiry", "Enter the expiry date as MM/YY.");
                } else if (isExpired(f.getExpiry().trim())) {
                    errors.put("expiry", "Card expiry date must be in the future.");
                }
                boolean amex = f.getCardNumber().startsWith("34") || f.getCardNumber().startsWith("37");
                if (!f.getCvv().matches(amex ? "^\\d{4}$" : "^\\d{3}$")) {
                    errors.put("cvv", "Invalid CVV. Please check again.");
                }
                if (StringUtils.hasText(f.getBillingAddress()) && f.getBillingAddress().trim().length() < 5) {
                    errors.put("billingAddress", "Billing address must be at least 5 characters.");
                }
            }
            case UPI -> {
                if (!f.getUpiId().trim().matches(Patterns.UPI)) errors.put("upiId", "Enter a valid UPI ID (for example name@bank).");
            }
            case WALLET -> {
                if (!WALLETS.contains(f.getWallet())) errors.put("wallet", "Select a wallet.");
            }
            case NET_BANKING -> {
                if (!BANKS.contains(f.getBank())) errors.put("bank", "Select your bank.");
            }
        }
        return errors;
    }

    private boolean isExpired(String mmYY) {
        int month = Integer.parseInt(mmYY.substring(0, 2));
        int year = 2000 + Integer.parseInt(mmYY.substring(3));
        return YearMonth.of(year, month).isBefore(YearMonth.now());
    }

    /** Charges the amount and records the attempt (successful or not). */
    @Transactional
    public Payment charge(Booking booking, BigDecimal amount, PaymentMethod method, PaymentForm f, String purpose) {
        Payment p = new Payment();
        p.setBooking(booking);
        p.setAmount(amount);
        p.setMethod(method);
        p.setPurpose(purpose);
        p.setTransactionId("TXN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%05d", RANDOM.nextInt(100000)));
        if (isCard(method)) {
            p.setCardLast4(f.getCardNumber().substring(f.getCardNumber().length() - 4));
            p.setPayerName(f.getCardHolder().trim());
        } else if (method == PaymentMethod.UPI) {
            p.setPayerName(f.getUpiId().trim());
        } else if (method == PaymentMethod.WALLET) {
            p.setPayerName(f.getWallet());
        } else {
            p.setPayerName(f.getBank());
        }
        String declined = callGateway(method, f);
        p.setSuccess(declined == null);
        p.setFailureReason(declined);
        return payments.save(p);
    }

    /** Gateway simulator. Returns null when the payment is approved, otherwise the decline reason. */
    private String callGateway(PaymentMethod method, PaymentForm f) {
        if (isCard(method) && f.getCardNumber().endsWith("0000")) return "Card declined by the issuing bank.";
        if (method == PaymentMethod.UPI && f.getUpiId().toLowerCase().startsWith("fail")) return "UPI request was rejected.";
        return null;
    }
}

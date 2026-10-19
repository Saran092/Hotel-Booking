package com.hotel.booking.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for in-flight payment sessions and modification drafts.
 * Replaces the HTTP session used by the old server-rendered app; keyed per user + booking.
 */
@Component
public class PendingPaymentStore {

    private final ConcurrentHashMap<String, Object> entries = new ConcurrentHashMap<>();

    private String key(String kind, Long userId, Long bookingId) {
        return kind + ":" + userId + ":" + bookingId;
    }

    public void putPayment(Long userId, Long bookingId, PaymentContext ctx) {
        entries.put(key("pay", userId, bookingId), ctx);
    }

    public PaymentContext getPayment(Long userId, Long bookingId) {
        Object o = entries.get(key("pay", userId, bookingId));
        return o instanceof PaymentContext p ? p : null;
    }

    public void clearPayment(Long userId, Long bookingId) {
        entries.remove(key("pay", userId, bookingId));
    }

    public void putDraft(Long userId, Long bookingId, ModificationDraft draft) {
        entries.put(key("mod", userId, bookingId), draft);
    }

    public ModificationDraft getDraft(Long userId, Long bookingId) {
        Object o = entries.get(key("mod", userId, bookingId));
        return o instanceof ModificationDraft d ? d : null;
    }

    public void clearDraft(Long userId, Long bookingId) {
        entries.remove(key("mod", userId, bookingId));
    }
}

package com.hotel.booking.service;

import com.hotel.booking.model.Notification;
import com.hotel.booking.model.User;
import com.hotel.booking.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Stores in-app notifications and logs the SMS / e-mail that would be sent.
 * Replace the log statement with a real SMS or mail gateway to send messages for real.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public void notify(User user, String message) {
        if (user == null) return;
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        repository.save(n);
        log.info("[SMS/E-mail to {} <{}>] {}", user.getUsername(), user.getEmail(), message);
    }

    public List<Notification> latest(User user) {
        return repository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return repository.countByUserAndReadFalse(user);
    }

    @Transactional
    public void markAllRead(User user) {
        List<Notification> unread = repository.findByUserAndReadFalse(user);
        unread.forEach(n -> n.setRead(true));
        repository.saveAll(unread);
    }
}

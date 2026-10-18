package com.hotel.booking.repository;

import com.hotel.booking.model.Notification;
import com.hotel.booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop10ByUserOrderByCreatedAtDesc(User user);

    long countByUserAndReadFalse(User user);

    List<Notification> findByUserAndReadFalse(User user);
}

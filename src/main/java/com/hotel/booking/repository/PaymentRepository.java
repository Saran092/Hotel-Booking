package com.hotel.booking.repository;

import com.hotel.booking.model.Booking;
import com.hotel.booking.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingOrderByCreatedAtDesc(Booking booking);

    Optional<Payment> findFirstByBookingAndSuccessTrueOrderByCreatedAtDesc(Booking booking);
}

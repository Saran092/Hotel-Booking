package com.hotel.booking.repository;

import com.hotel.booking.model.Bill;
import com.hotel.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findAllByOrderByIssueDateDesc();

    List<Bill> findByBooking(Booking booking);
}

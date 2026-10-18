package com.hotel.booking.repository;

import com.hotel.booking.model.Booking;
import com.hotel.booking.model.BookingStatus;
import com.hotel.booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    String ACTIVE = "(b.status in (com.hotel.booking.model.BookingStatus.CONFIRMED, com.hotel.booking.model.BookingStatus.CHECKED_IN) "
            + "or (b.status = com.hotel.booking.model.BookingStatus.PENDING_PAYMENT and b.holdExpiresAt > :now))";

    Optional<Booking> findByBookingRef(String bookingRef);

    List<Booking> findByCustomerOrderByCheckInDateDesc(User customer);

    List<Booking> findAllByOrderByCreatedAtDesc();

    long countByRoomIdAndStatusInAndCheckOutDateGreaterThanEqual(Long roomId, Collection<BookingStatus> statuses, LocalDate date);

    boolean existsByRoomId(Long roomId);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    /** Number of active reservations that overlap the given stay for a room (the booking with excludeId is ignored). */
    @Query("select count(b) from Booking b where b.room.id = :roomId and b.checkInDate < :checkOut and b.checkOutDate > :checkIn "
            + "and b.id <> :excludeId and " + ACTIVE)
    long countOverlapping(@Param("roomId") Long roomId, @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut,
                          @Param("excludeId") Long excludeId, @Param("now") LocalDateTime now);

    /** IDs of rooms that are taken for at least one night of the given stay. */
    @Query("select distinct b.room.id from Booking b where b.checkInDate < :checkOut and b.checkOutDate > :checkIn and " + ACTIVE)
    List<Long> findBookedRoomIds(@Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut,
                                 @Param("now") LocalDateTime now);
}

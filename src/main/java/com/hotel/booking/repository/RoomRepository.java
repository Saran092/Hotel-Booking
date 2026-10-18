package com.hotel.booking.repository;

import com.hotel.booking.model.Room;
import com.hotel.booking.model.RoomStatus;
import com.hotel.booking.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    List<Room> findByRoomTypeAndStatusNot(RoomType type, RoomStatus status);
}

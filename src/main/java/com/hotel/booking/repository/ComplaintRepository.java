package com.hotel.booking.repository;

import com.hotel.booking.model.Complaint;
import com.hotel.booking.model.ComplaintStatus;
import com.hotel.booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByCustomerOrderBySubmittedAtDesc(User customer);

    List<Complaint> findAllByOrderBySubmittedAtDesc();

    List<Complaint> findByAssignedToOrderBySubmittedAtDesc(User staff);

    long countByAssignedToAndStatusIn(User staff, Collection<ComplaintStatus> statuses);

    Optional<Complaint> findByReference(String reference);
}

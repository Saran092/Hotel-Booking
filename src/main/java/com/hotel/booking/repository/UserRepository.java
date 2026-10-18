package com.hotel.booking.repository;

import com.hotel.booking.model.Role;
import com.hotel.booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByCountryCodeAndMobile(String countryCode, String mobile);

    boolean existsByCountryCodeAndMobileAndIdNot(String countryCode, String mobile, Long id);

    Optional<User> findByUsernameIgnoreCaseAndEmailIgnoreCase(String username, String email);

    @Query("select distinct u from User u join u.roles r where r = :role order by u.fullName")
    List<User> findByRole(@Param("role") Role role);
}

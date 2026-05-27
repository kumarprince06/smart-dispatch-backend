package com.smartdispatch.auth.repository;

import com.smartdispatch.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role.name = 'ROLE_CUSTOMER' " +
           "AND (:status IS NULL OR (:status = 'ACTIVE' AND u.active = true) OR (:status = 'INACTIVE' AND u.active = false)) " +
           "AND (:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(u.phoneNo) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<User> findAllCustomers(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}

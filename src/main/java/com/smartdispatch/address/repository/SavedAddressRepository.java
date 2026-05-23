package com.smartdispatch.address.repository;

import com.smartdispatch.address.entity.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {

    List<SavedAddress> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SavedAddress> findByUserIdAndIsDefaultTrue(Long userId);

    Long countByUserId(Long userId);
}

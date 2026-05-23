package com.smartdispatch.driver.repository;

import com.smartdispatch.driver.entity.Driver;
import com.smartdispatch.driver.enums.DriverStatus;
import com.smartdispatch.driver.enums.DriverTier;
import com.smartdispatch.driver.enums.VehicleType;
import com.smartdispatch.driver.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    Optional<Driver> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    Optional<Driver> findByUserEmail(String email);

    Boolean existsByVehicleNumber(String vehicleNumber);

    Boolean existsByLicenseNumber(String licenseNumber);

    Boolean existsByUserId(Long userId);

    // Find drivers by status
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    List<Driver> findByStatus(DriverStatus status);

    // Find available drivers by vehicle type
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    List<Driver> findByStatusAndVehicleType(DriverStatus status, VehicleType vehicleType);

    // Paginated listing with filters
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    @Query("SELECT d FROM Driver d WHERE " +
            "(:status IS NULL OR d.status = :status) AND " +
            "(:vehicleType IS NULL OR d.vehicleType = :vehicleType) AND " +
            "(:verificationStatus IS NULL OR d.verificationStatus = :verificationStatus) AND " +
            "(:tier IS NULL OR d.tier = :tier) AND " +
            "(:active IS NULL OR d.active = :active) AND " +
            "(:search IS NULL OR LOWER(d.user.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(d.user.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(d.user.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(d.vehicleNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Driver> findAllWithFilters(
            @Param("status") DriverStatus status,
            @Param("vehicleType") VehicleType vehicleType,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("tier") DriverTier tier,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );

    // Find nearby available drivers using Haversine formula
    @Query("SELECT d FROM Driver d WHERE " +
            "d.status = 'AVAILABLE' AND " +
            "d.verificationStatus = 'VERIFIED' AND " +
            "d.active = true AND " +
            "d.currentLatitude IS NOT NULL AND " +
            "d.currentLongitude IS NOT NULL AND " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(d.currentLatitude)) * " +
            "cos(radians(d.currentLongitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(d.currentLatitude)))) <= :radiusKm " +
            "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(d.currentLatitude)) * " +
            "cos(radians(d.currentLongitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(d.currentLatitude))))")
    List<Driver> findNearbyAvailableDrivers(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("radiusKm") Double radiusKm
    );

    // Count by status
    Long countByStatus(DriverStatus status);

    // Count by verification status
    Long countByVerificationStatus(VerificationStatus verificationStatus);

    // Count active drivers
    Long countByActiveTrue();

    // Find by status excluding soft-deleted drivers
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    Page<Driver> findByStatusAndActiveTrue(DriverStatus status, Pageable pageable);

    // Count by status only for active drivers
    Long countByStatusAndActiveTrue(DriverStatus status);
}

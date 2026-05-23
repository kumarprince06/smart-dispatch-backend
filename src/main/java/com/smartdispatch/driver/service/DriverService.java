package com.smartdispatch.driver.service;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.driver.dto.*;
import com.smartdispatch.driver.entity.Driver;
import com.smartdispatch.driver.enums.*;
import com.smartdispatch.driver.mapper.DriverMapper;
import com.smartdispatch.driver.repository.DriverRepository;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DriverMapper driverMapper;

    // ═══════════════════════════════════════════
    // Onboard New Driver
    // ═══════════════════════════════════════════
    @Transactional
    public DriverResponse createDriver(CreateDriverRequest request) {

        // Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found with ID: " + request.getUserId()));

        // Check if driver profile already exists for this user
        if (driverRepository.existsByUserId(request.getUserId())) {
            throw new BadRequestException("Driver profile already exists for this user");
        }

        // Check vehicle number uniqueness
        if (driverRepository.existsByVehicleNumber(request.getVehicleNumber())) {
            throw new BadRequestException("Vehicle number already registered: " + request.getVehicleNumber());
        }

        // Check license number uniqueness
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new BadRequestException("License number already registered: " + request.getLicenseNumber());
        }

        Driver driver = driverMapper.toEntity(request, user);
        Driver savedDriver = driverRepository.save(driver);

        log.info("Driver onboarded successfully. ID: {}, User: {}", savedDriver.getId(), user.getEmail());

        return driverMapper.toResponse(savedDriver);
    }

    // ═══════════════════════════════════════════
    // Get Driver by ID
    // ═══════════════════════════════════════════
    public DriverResponse getDriverById(Long id) {
        Driver driver = findDriverOrThrow(id);
        return driverMapper.toResponse(driver);
    }

    // ═══════════════════════════════════════════
    // List All Drivers (Paginated + Filtered)
    // ═══════════════════════════════════════════
    public Page<DriverResponse> getAllDrivers(
            int page,
            int size,
            String sortBy,
            String sortDir,
            DriverStatus status,
            VehicleType vehicleType,
            VerificationStatus verificationStatus,
            DriverTier tier,
            Boolean active,
            String search
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Driver> drivers = driverRepository.findAllWithFilters(
                status, vehicleType, verificationStatus, tier, active, search, pageable
        );

        return drivers.map(driverMapper::toResponse);
    }

    // ═══════════════════════════════════════════
    // Update Driver Profile
    // ═══════════════════════════════════════════
    @Transactional
    public DriverResponse updateDriver(Long id, UpdateDriverRequest request) {
        Driver driver = findDriverOrThrow(id);

        // Personal
        if (request.getProfilePictureUrl() != null) driver.setProfilePictureUrl(request.getProfilePictureUrl());
        if (request.getDateOfBirth() != null) driver.setDateOfBirth(request.getDateOfBirth());
        if (request.getAddress() != null) driver.setAddress(request.getAddress());
        if (request.getCity() != null) driver.setCity(request.getCity());
        if (request.getState() != null) driver.setState(request.getState());
        if (request.getPincode() != null) driver.setPincode(request.getPincode());
        if (request.getEmergencyContactName() != null) driver.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) driver.setEmergencyContactPhone(request.getEmergencyContactPhone());

        // Vehicle
        if (request.getVehicleType() != null) driver.setVehicleType(request.getVehicleType());
        if (request.getVehicleNumber() != null) {
            if (!request.getVehicleNumber().equals(driver.getVehicleNumber())
                    && driverRepository.existsByVehicleNumber(request.getVehicleNumber())) {
                throw new BadRequestException("Vehicle number already registered");
            }
            driver.setVehicleNumber(request.getVehicleNumber());
        }
        if (request.getVehicleModel() != null) driver.setVehicleModel(request.getVehicleModel());
        if (request.getVehicleColor() != null) driver.setVehicleColor(request.getVehicleColor());
        if (request.getVehicleYear() != null) driver.setVehicleYear(request.getVehicleYear());
        if (request.getVehicleCapacityKg() != null) driver.setVehicleCapacityKg(request.getVehicleCapacityKg());

        // License
        if (request.getLicenseNumber() != null) {
            if (!request.getLicenseNumber().equals(driver.getLicenseNumber())
                    && driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                throw new BadRequestException("License number already registered");
            }
            driver.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getLicenseExpiry() != null) driver.setLicenseExpiry(request.getLicenseExpiry());

        // Zone
        if (request.getPreferredZone() != null) driver.setPreferredZone(request.getPreferredZone());
        if (request.getServiceRadiusKm() != null) driver.setServiceRadiusKm(request.getServiceRadiusKm());

        // Capacity
        if (request.getMaxConcurrentOrders() != null) driver.setMaxConcurrentOrders(request.getMaxConcurrentOrders());

        // Skill Tags
        if (request.getSkillTags() != null) driver.setSkillTags(request.getSkillTags());

        Driver updatedDriver = driverRepository.save(driver);
        log.info("Driver updated. ID: {}", id);

        return driverMapper.toResponse(updatedDriver);
    }

    // ═══════════════════════════════════════════
    // Soft Delete Driver
    // ═══════════════════════════════════════════
    @Transactional
    public void deleteDriver(Long id) {
        Driver driver = findDriverOrThrow(id);
        driver.setActive(false);
        driver.setStatus(DriverStatus.OFFLINE);
        driverRepository.save(driver);
        log.info("Driver soft-deleted. ID: {}", id);
    }

    // ═══════════════════════════════════════════
    // Update Driver Status
    // ═══════════════════════════════════════════
    @Transactional
    public DriverResponse updateDriverStatus(Long id, UpdateDriverStatusRequest request) {
        Driver driver = findDriverOrThrow(id);

        // Cannot go online if not verified
        if (request.getStatus() == DriverStatus.AVAILABLE
                && driver.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BadRequestException("Driver must be verified before going online");
        }

        // Cannot go online if suspended
        if (request.getStatus() == DriverStatus.AVAILABLE
                && driver.getStatus() == DriverStatus.SUSPENDED) {
            throw new BadRequestException("Suspended drivers cannot go online. Contact support.");
        }

        driver.setStatus(request.getStatus());

        if (request.getStatus() == DriverStatus.AVAILABLE) {
            driver.setLastActiveAt(LocalDateTime.now());
        }

        Driver updated = driverRepository.save(driver);
        log.info("Driver status updated. ID: {}, Status: {}", id, request.getStatus());

        return driverMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════
    // Update Real-time Location
    // ═══════════════════════════════════════════
    @Transactional
    public DriverResponse updateDriverLocation(Long id, UpdateDriverLocationRequest request) {
        Driver driver = findDriverOrThrow(id);

        driver.setCurrentLatitude(request.getLatitude());
        driver.setCurrentLongitude(request.getLongitude());
        driver.setLocationUpdatedAt(LocalDateTime.now());
        driver.setLastActiveAt(LocalDateTime.now());

        Driver updated = driverRepository.save(driver);

        return driverMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════
    // Verify / Reject Driver (Admin)
    // ═══════════════════════════════════════════
    @Transactional
    public DriverResponse verifyDriver(Long id, VerificationStatus status, String rejectionReason) {
        Driver driver = findDriverOrThrow(id);

        driver.setVerificationStatus(status);

        if (status == VerificationStatus.REJECTED) {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new BadRequestException("Rejection reason is required");
            }
            driver.setRejectionReason(rejectionReason);
            driver.setStatus(DriverStatus.OFFLINE);
        } else if (status == VerificationStatus.VERIFIED) {
            driver.setRejectionReason(null);
        }

        Driver updated = driverRepository.save(driver);
        log.info("Driver verification updated. ID: {}, Status: {}", id, status);

        return driverMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════
    // Find Nearby Available Drivers
    // ═══════════════════════════════════════════
    public List<DriverResponse> findNearbyDrivers(Double latitude, Double longitude, Double radiusKm) {

        if (radiusKm == null || radiusKm <= 0) {
            radiusKm = 5.0; // Default 5km radius
        }

        List<Driver> drivers = driverRepository.findNearbyAvailableDrivers(latitude, longitude, radiusKm);

        return drivers.stream()
                .map(driverMapper::toResponse)
                .toList();
    }

    // ═══════════════════════════════════════════
    // Driver Stats (Admin Dashboard)
    // ═══════════════════════════════════════════
    public DriverStatsResponse getDriverStats() {
        return DriverStatsResponse.builder()
                .totalDrivers(driverRepository.countByActiveTrue())
                .availableDrivers(driverRepository.countByStatus(DriverStatus.AVAILABLE))
                .busyDrivers(driverRepository.countByStatus(DriverStatus.BUSY))
                .offlineDrivers(driverRepository.countByStatus(DriverStatus.OFFLINE))
                .onlineDrivers(driverRepository.countByStatus(DriverStatus.ONLINE))
                .onDeliveryDrivers(driverRepository.countByStatus(DriverStatus.ON_DELIVERY))
                .onBreakDrivers(driverRepository.countByStatus(DriverStatus.ON_BREAK))
                .suspendedDrivers(driverRepository.countByStatus(DriverStatus.SUSPENDED))
                .pendingVerification(driverRepository.countByVerificationStatus(VerificationStatus.PENDING))
                .verifiedDrivers(driverRepository.countByVerificationStatus(VerificationStatus.VERIFIED))
                .rejectedDrivers(driverRepository.countByVerificationStatus(VerificationStatus.REJECTED))
                .build();
    }

    // ═══════════════════════════════════════════
    // Get My Profile (JWT-based, driver self-service)
    // ═══════════════════════════════════════════
    public DriverResponse getMyProfile() {
        String email = SecurityUtil.getCurrentUserEmail();
        Driver driver = driverRepository.findByUserEmail(email)
                .orElseThrow(() -> new BadRequestException("Driver profile not found"));
        return driverMapper.toResponse(driver);
    }

    // ═══════════════════════════════════════════
    // Update My Status (JWT-based)
    // ═══════════════════════════════════════════
    @Transactional
    public DriverResponse updateMyStatus(UpdateDriverStatusRequest request) {
        String email = SecurityUtil.getCurrentUserEmail();
        Driver driver = driverRepository.findByUserEmail(email)
                .orElseThrow(() -> new BadRequestException("Driver not found"));

        if (request.getStatus() == DriverStatus.AVAILABLE
                && driver.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BadRequestException("Driver must be verified before going online");
        }

        driver.setStatus(request.getStatus());
        if (request.getStatus() == DriverStatus.AVAILABLE || request.getStatus() == DriverStatus.ONLINE) {
            driver.setLastActiveAt(LocalDateTime.now());
        }

        Driver updated = driverRepository.save(driver);
        log.info("Driver status updated: {} -> {}", email, request.getStatus());
        return driverMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════
    // Update My Location (JWT-based)
    // ═══════════════════════════════════════════
    @Transactional
    public DriverResponse updateMyLocation(UpdateDriverLocationRequest request) {
        String email = SecurityUtil.getCurrentUserEmail();
        Driver driver = driverRepository.findByUserEmail(email)
                .orElseThrow(() -> new BadRequestException("Driver not found"));

        driver.setCurrentLatitude(request.getLatitude());
        driver.setCurrentLongitude(request.getLongitude());
        driver.setLocationUpdatedAt(LocalDateTime.now());
        driver.setLastActiveAt(LocalDateTime.now());

        Driver updated = driverRepository.save(driver);
        return driverMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════
    // Update Driver Rating (weighted average)
    // ═══════════════════════════════════════════
    @Transactional
    public void updateDriverRating(Long driverId, Double newRating) {
        if (newRating < 1 || newRating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }

        Driver driver = findDriverOrThrow(driverId);

        double totalScore = driver.getRating() * driver.getTotalRatings();
        totalScore += newRating;
        int updatedTotalRatings = driver.getTotalRatings() + 1;
        double updatedAverage = totalScore / updatedTotalRatings;

        driver.setRating(Math.round(updatedAverage * 100.0) / 100.0);
        driver.setTotalRatings(updatedTotalRatings);
        driver.calculateTier();

        driverRepository.save(driver);
        log.info("Driver rating updated. ID: {}, New Avg: {}", driverId, updatedAverage);
    }

    // ═══════════════════════════════════════════
    // Increment Active Orders
    // ═══════════════════════════════════════════
    @Transactional
    public void incrementActiveOrders(Long driverId) {
        Driver driver = findDriverOrThrow(driverId);
        driver.setActiveOrderCount(driver.getActiveOrderCount() + 1);

        if (driver.getActiveOrderCount() >= driver.getMaxConcurrentOrders()) {
            driver.setStatus(DriverStatus.BUSY);
        }

        driverRepository.save(driver);
        log.info("Driver active orders incremented. ID: {}, Count: {}", driverId, driver.getActiveOrderCount());
    }

    // ═══════════════════════════════════════════
    // Decrement Active Orders
    // ═══════════════════════════════════════════
    @Transactional
    public void decrementActiveOrders(Long driverId) {
        Driver driver = findDriverOrThrow(driverId);
        driver.setActiveOrderCount(Math.max(0, driver.getActiveOrderCount() - 1));

        if (driver.getActiveOrderCount() < driver.getMaxConcurrentOrders()
                && driver.getStatus() == DriverStatus.BUSY) {
            driver.setStatus(DriverStatus.AVAILABLE);
        }

        driverRepository.save(driver);
        log.info("Driver active orders decremented. ID: {}, Count: {}", driverId, driver.getActiveOrderCount());
    }

    // ═══════════════════════════════════════════
    // Helper
    // ═══════════════════════════════════════════
    private Driver findDriverOrThrow(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Driver not found with ID: " + id));
    }
}


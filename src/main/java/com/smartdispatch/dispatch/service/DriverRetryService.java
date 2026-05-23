package com.smartdispatch.dispatch.service;

import com.smartdispatch.dispatch.dto.DispatchResult;
import com.smartdispatch.dispatch.enums.DispatchStrategy;
import com.smartdispatch.driver.service.DriverService;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.order.entity.Order;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.repository.OrderRepository;
import com.smartdispatch.driver.entity.Driver;
import com.smartdispatch.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Driver Retry Service.
 * When a driver rejects or doesn't respond within timeout,
 * automatically re-assigns to the next best driver.
 *
 * Also handles scheduled order dispatch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverRetryService {

    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final DriverService driverService;
    private final DispatchService dispatchService;
    private final org.redisson.api.RedissonClient redissonClient;

    private static final int ASSIGNMENT_TIMEOUT_SECONDS = 60; // Driver must accept within 60s

    // ═══════════════════════════════════════════
    // Retry: Re-assign unaccepted orders
    // Runs every 30 seconds
    // ═══════════════════════════════════════════
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void retryUnassignedOrders() {
        // Find orders stuck in CREATED status
        List<Order> unassigned = orderRepository.findByStatus(
                OrderStatus.CREATED,
                org.springframework.data.domain.PageRequest.of(0, 50)
        ).getContent();

        for (Order order : unassigned) {
            if (order.getAssignmentAttempts() >= order.getMaxAssignmentAttempts()) {
                log.warn("Order {} exceeded max assignment attempts ({}). Needs manual review.",
                        order.getId(), order.getMaxAssignmentAttempts());
                continue;
            }

            // Skip if not enough time since last attempt
            if (order.getLastAssignmentAttemptAt() != null &&
                    order.getLastAssignmentAttemptAt().plusSeconds(ASSIGNMENT_TIMEOUT_SECONDS).isAfter(LocalDateTime.now())) {
                continue;
            }

            org.redisson.api.RLock lock = redissonClient.getLock("order-dispatch-lock:" + order.getId());
            boolean isLocked = false;
            try {
                // Try to acquire lock for 5 seconds, hold for 10 seconds max
                isLocked = lock.tryLock(5, 10, java.util.concurrent.TimeUnit.SECONDS);
                if (!isLocked) {
                    log.debug("Order {} is currently being dispatched by another process. Skipping.", order.getId());
                    continue;
                }

                // Double check status inside lock
                Order lockedOrder = orderRepository.findById(order.getId()).orElse(order);
                if (lockedOrder.getStatus() != OrderStatus.CREATED) {
                     continue;
                }

                DispatchResult result = dispatchService.findNearestDriver(
                        lockedOrder.getPickupLatitude(), lockedOrder.getPickupLongitude(),
                        DispatchStrategy.NEAREST
                );

                Driver driver = driverRepository.findById(result.getDriverId())
                        .orElse(null);

                if (driver != null) {
                    lockedOrder.setDriver(driver);
                    lockedOrder.setStatus(OrderStatus.ASSIGNED);
                    lockedOrder.setAssignedAt(LocalDateTime.now());
                    lockedOrder.setEstimatedDeliveryAt(LocalDateTime.now().plusMinutes(result.getEstimatedMinutes()));
                    lockedOrder.setAssignmentAttempts(lockedOrder.getAssignmentAttempts() + 1);
                    lockedOrder.setLastAssignmentAttemptAt(LocalDateTime.now());

                    driverService.incrementActiveOrders(driver.getId());
                    orderRepository.save(lockedOrder);

                    log.info("Retry SUCCESS: Order {} assigned to driver {} (attempt {})",
                            lockedOrder.getId(), driver.getId(), lockedOrder.getAssignmentAttempts());
                }
            } catch (BadRequestException e) {
                order.setAssignmentAttempts(order.getAssignmentAttempts() + 1);
                order.setLastAssignmentAttemptAt(LocalDateTime.now());
                orderRepository.save(order);

                log.warn("Retry FAILED: Order {} — attempt {}/{}. Reason: {}",
                        order.getId(), order.getAssignmentAttempts(),
                        order.getMaxAssignmentAttempts(), e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Lock interrupted for order {}", order.getId(), e);
            } finally {
                if (isLocked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    // Scheduled Order Dispatcher
    // Runs every 60 seconds — dispatches orders
    // whose scheduledAt time has arrived
    // ═══════════════════════════════════════════
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void dispatchScheduledOrders() {
        List<Order> scheduledOrders = orderRepository.findScheduledOrdersDue(LocalDateTime.now());

        for (Order order : scheduledOrders) {
            org.redisson.api.RLock lock = redissonClient.getLock("order-dispatch-lock:" + order.getId());
            boolean isLocked = false;
            try {
                isLocked = lock.tryLock(5, 10, java.util.concurrent.TimeUnit.SECONDS);
                if (!isLocked) {
                     continue;
                }

                Order lockedOrder = orderRepository.findById(order.getId()).orElse(order);
                if (lockedOrder.getStatus() != OrderStatus.CREATED) {
                     continue;
                }

                DispatchResult result = dispatchService.findNearestDriver(
                        lockedOrder.getPickupLatitude(), lockedOrder.getPickupLongitude(),
                        DispatchStrategy.NEAREST
                );

                Driver driver = driverRepository.findById(result.getDriverId())
                        .orElse(null);

                if (driver != null) {
                    lockedOrder.setDriver(driver);
                    lockedOrder.setStatus(OrderStatus.ASSIGNED);
                    lockedOrder.setAssignedAt(LocalDateTime.now());
                    lockedOrder.setEstimatedDeliveryAt(LocalDateTime.now().plusMinutes(result.getEstimatedMinutes()));

                    driverService.incrementActiveOrders(driver.getId());
                    orderRepository.save(lockedOrder);

                    log.info("Scheduled order {} dispatched to driver {}",
                            lockedOrder.getId(), driver.getId());
                }
            } catch (BadRequestException e) {
                log.warn("Scheduled order {} — no driver available: {}",
                        order.getId(), e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Lock interrupted for scheduled order {}", order.getId(), e);
            } finally {
                if (isLocked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}

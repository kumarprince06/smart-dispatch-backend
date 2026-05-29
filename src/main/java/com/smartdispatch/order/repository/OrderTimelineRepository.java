package com.smartdispatch.order.repository;

import com.smartdispatch.order.entity.OrderTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, Long> {

    List<OrderTimeline> findByOrderIdOrderByTimestampAsc(Long orderId);
}

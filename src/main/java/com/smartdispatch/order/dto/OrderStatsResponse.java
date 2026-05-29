package com.smartdispatch.order.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsResponse {

    private Long totalOrders;
    private Long createdOrders;
    private Long assignedOrders;
    private Long pickedUpOrders;
    private Long inTransitOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;
    private Long failedOrders;
}

package com.smartdispatch.order.enums;

public enum OrderStatus {
    REQUESTED,        // Order created, awaiting payment
    PAYMENT_PENDING,  // Waiting for payment to be processed
    PAYMENT_FAILED,   // Payment failed
    CONFIRMED,        // Payment received, order confirmed
    ASSIGNED,         // Driver assigned
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED,
    FAILED
}

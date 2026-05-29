package com.smartdispatch.notification.enums;

/**
 * Predefined notification templates.
 * Each template has a subject + body pattern.
 */
public enum NotificationTemplate {

    ORDER_CREATED("Order Placed", "Your order %s has been placed successfully!"),
    ORDER_ASSIGNED("Driver Assigned", "Driver %s has been assigned to your order %s"),
    ORDER_PICKED_UP("Package Picked Up", "Your package for order %s has been picked up!"),
    ORDER_IN_TRANSIT("On The Way", "Your package for order %s is on the way!"),
    ORDER_DELIVERED("Delivered!", "Your order %s has been delivered successfully!"),
    ORDER_CANCELLED("Order Cancelled", "Your order %s has been cancelled. Reason: %s"),

    DRIVER_ASSIGNED_NEW_ORDER("New Order!", "You have been assigned a new order %s. Pickup: %s"),
    DRIVER_ORDER_CANCELLED("Order Cancelled", "Order %s has been cancelled by the customer"),

    PAYMENT_SUCCESS("Payment Received", "Payment of ₹%s received for order %s"),
    PAYMENT_FAILED("Payment Failed", "Payment of ₹%s failed for order %s. Please retry."),
    PAYMENT_REFUND("Refund Processed", "Refund of ₹%s processed for order %s"),

    WALLET_CREDITED("Wallet Credited", "₹%s has been added to your wallet. Balance: ₹%s"),
    WALLET_DEBITED("Wallet Debited", "₹%s deducted from wallet for order %s"),

    WELCOME("Welcome to Smart Dispatch!", "Hi %s, welcome to Smart Dispatch! Start your first delivery today."),
    PASSWORD_RESET("Password Reset", "Your password has been reset successfully.");

    private final String subject;
    private final String bodyTemplate;

    NotificationTemplate(String subject, String bodyTemplate) {
        this.subject = subject;
        this.bodyTemplate = bodyTemplate;
    }

    public String getSubject() { return subject; }
    public String formatBody(Object... args) { return String.format(bodyTemplate, args); }
}

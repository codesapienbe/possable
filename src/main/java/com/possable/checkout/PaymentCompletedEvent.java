package com.possable.checkout;

import java.time.Instant;

import org.springframework.modulith.events.Externalized;

/**
 * Domain event published when a payment is completed.
 * Other modules can listen to this for post-payment processing.
 */
@Externalized("payment.completed::#{id()}")
public record PaymentCompletedEvent(
    String id,
    String orderId,
    double amount,
    String method,
    Instant completedAt
) {
    public PaymentCompletedEvent {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative");
        }
        if (completedAt == null) {
            completedAt = Instant.now();
        }
    }
} 
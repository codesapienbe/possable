package com.possable.order;

import java.time.Instant;

import org.springframework.modulith.events.Externalized;

/**
 * Domain event published when an order is completed.
 * Checkout module listens to this to initiate payment processing.
 */
@Externalized("order.completed::#{id()}")
public record OrderCompletedEvent(
    String id,
    String status,
    Instant completedAt
) {
    public OrderCompletedEvent {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (completedAt == null) {
            completedAt = Instant.now();
        }
    }
} 
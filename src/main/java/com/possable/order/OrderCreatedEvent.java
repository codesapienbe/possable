package com.possable.order;

import java.time.Instant;
import java.util.List;

import org.springframework.modulith.events.Externalized;

/**
 * Domain event published when an order is created.
 * Other modules can listen to this event to react to order creation.
 */
@Externalized("order.created::#{id()}")
public record OrderCreatedEvent(
    String id,
    List<String> itemIds,
    String notes,
    String status,
    Instant createdAt
) {
    public OrderCreatedEvent {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (itemIds == null) {
            throw new IllegalArgumentException("Item IDs cannot be null");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
} 
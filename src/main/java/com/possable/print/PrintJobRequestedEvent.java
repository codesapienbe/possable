package com.possable.print;

import java.time.Instant;
import java.util.List;

import org.springframework.modulith.events.Externalized;

/**
 * Domain event published when a print job is requested.
 * Print module listens to this to create and process print jobs.
 */
@Externalized("print.requested::#{orderId()}")
public record PrintJobRequestedEvent(
    String orderId,
    List<String> itemIds,
    List<String> printerCategories,
    Instant requestedAt
) {
    public PrintJobRequestedEvent {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (printerCategories == null || printerCategories.isEmpty()) {
            throw new IllegalArgumentException("Printer categories cannot be null or empty");
        }
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
    }
} 
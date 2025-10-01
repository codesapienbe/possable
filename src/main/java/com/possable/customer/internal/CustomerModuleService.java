package com.possable.customer.internal;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.possable.order.OrderCreatedEvent;
import com.possable.checkout.PaymentCompletedEvent;

/**
 * Internal service for customer module.
 * Handles customer-related operations and listens to relevant events.
 */
@Service
public class CustomerModuleService {

    private static final Logger log = LoggerFactory.getLogger(CustomerModuleService.class);

    /**
     * Listen to order created events to notify customers
     */
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("{\"message\":\"order_created_event_received\", \"order_id\":\"{}\", \"component\":\"customer-module\", \"timestamp\":\"{}\"}", 
            event.id(), Instant.now());
        // Business logic: send customer notification, update customer order history, etc.
    }

    /**
     * Listen to payment completed events to send customer receipts
     */
    @EventListener
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("{\"message\":\"payment_completed_event_received\", \"payment_id\":\"{}\", \"order_id\":\"{}\", \"component\":\"customer-module\", \"timestamp\":\"{}\"}", 
            event.id(), event.orderId(), Instant.now());
        // Business logic: send customer receipt, update loyalty points, etc.
    }
} 

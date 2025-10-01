package com.possable.checkout.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.possable.checkout.PaymentCompletedEvent;
import com.possable.order.OrderCompletedEvent;

/**
 * Internal service for checkout module.
 * Handles payment processing and publishes payment events.
 */
@Service
public class CheckoutModuleService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutModuleService.class);

    private final List<Payment> payments = Collections.synchronizedList(new ArrayList<>());
    private final TaskExecutor taskExecutor;
    private final ApplicationEventPublisher eventPublisher;

    public record Payment(String id, String orderId, double amount, String method, String status, Instant paidAt) {}

    public CheckoutModuleService(TaskExecutor taskExecutor, ApplicationEventPublisher eventPublisher) {
        this.taskExecutor = taskExecutor;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Listen to order completed events and potentially trigger automatic payment processing
     */
    @EventListener
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("{\"message\":\"order_completed_event_received\", \"order_id\":\"{}\", \"component\":\"checkout-module\", \"timestamp\":\"{}\"}", 
            event.id(), Instant.now());
        // Business logic could auto-initiate payment or just log for audit
    }

    public Payment createPayment(String orderId, double amount, String method) {
        String id = UUID.randomUUID().toString();
        Payment p = new Payment(id, orderId, amount, method, "pending", null);
        payments.add(p);
        
        log.info("{\"message\":\"payment_created\", \"payment_id\":\"{}\", \"order_id\":\"{}\", \"amount\":{}, \"method\":\"{}\", \"component\":\"checkout-module\", \"timestamp\":\"{}\"}", 
            id, orderId, amount, method, Instant.now());

        CompletableFuture.runAsync(() -> processPayment(id), taskExecutor::execute)
            .exceptionally(ex -> {
                log.error("{\"message\":\"payment_processing_failed\", \"payment_id\":\"{}\", \"component\":\"checkout-module\", \"error\":\"{}\"}", 
                    id, ex.getMessage(), ex);
                return null;
            });

        return p;
    }

    public Payment findById(String id) {
        synchronized (payments) {
            return payments.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElse(null);
        }
    }

    private void processPayment(String paymentId) {
        try {
            Thread.sleep(100);
            synchronized (payments) {
                for (int i = 0; i < payments.size(); i++) {
                    var p = payments.get(i);
                    if (p.id().equals(paymentId)) {
                        Instant completedAt = Instant.now();
                        payments.set(i, new Payment(p.id(), p.orderId(), p.amount(), p.method(), "completed", completedAt));
                        
                        log.info("{\"message\":\"payment_completed\", \"payment_id\":\"{}\", \"order_id\":\"{}\", \"component\":\"checkout-module\", \"timestamp\":\"{}\"}", 
                            paymentId, p.orderId(), Instant.now());
                        
                        // Publish payment completed event
                        PaymentCompletedEvent event = new PaymentCompletedEvent(
                            p.id(),
                            p.orderId(),
                            p.amount(),
                            p.method(),
                            completedAt
                        );
                        eventPublisher.publishEvent(event);
                        log.debug("{\"message\":\"payment_completed_event_published\", \"payment_id\":\"{}\", \"component\":\"checkout-module\"}", paymentId);
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            synchronized (payments) {
                for (int i = 0; i < payments.size(); i++) {
                    var p = payments.get(i);
                    if (p.id().equals(paymentId)) {
                        payments.set(i, new Payment(p.id(), p.orderId(), p.amount(), p.method(), "failed", p.paidAt()));
                        log.error("{\"message\":\"payment_processing_interrupted\", \"payment_id\":\"{}\", \"component\":\"checkout-module\"}", paymentId);
                        break;
                    }
                }
            }
        }
    }

    public List<Payment> listPayments() {
        synchronized (payments) {
            return List.copyOf(payments);
        }
    }
} 

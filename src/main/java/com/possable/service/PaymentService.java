package com.possable.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final List<Payment> payments = Collections.synchronizedList(new ArrayList<>());
    private final TaskExecutor taskExecutor;

    public record Payment(String id, String orderId, double amount, String method, String status, Instant paidAt) {}

    public PaymentService(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public Payment createPayment(String orderId, double amount, String method) {
        String id = UUID.randomUUID().toString();
        Payment p = new Payment(id, orderId, amount, method, "pending", null);
        payments.add(p);
        log.info("{\"message\":\"payment_created\", \"payment_id\":\"{}\", \"component\":\"payment-service\"}", id);

        CompletableFuture.runAsync(() -> processPayment(id), taskExecutor::execute)
                .exceptionally(ex -> {
                    log.error("payment processing failed for {}", id, ex);
                    return null;
                });

        return p;
    }

    public Payment findById(String id) {
        synchronized (payments) {
            return payments.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
        }
    }

    private void processPayment(String paymentId) {
        try {
            // mark completed for this skeleton
            Thread.sleep(100);
            synchronized (payments) {
                for (int i = 0; i < payments.size(); i++) {
                    var p = payments.get(i);
                    if (p.id().equals(paymentId)) {
                        payments.set(i, new Payment(p.id(), p.orderId(), p.amount(), p.method(), "completed", Instant.now()));
                        log.info("{\"message\":\"payment_completed\", \"payment_id\":\"{}\", \"component\":\"payment-service\"}", paymentId);
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
                        break;
                    }
                }
            }
        }
    }
} 
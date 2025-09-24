package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentServiceFinalTest {

    private final TaskExecutor syncExecutor = r -> r.run();

    @Test
    public void multiplePaymentsAndFind() throws InterruptedException {
        PaymentService svc = new PaymentService(syncExecutor);
        var p1 = svc.createPayment("o1", 1.0, "card");
        var p2 = svc.createPayment("o2", 2.0, "card");
        assertNotNull(svc.findById(p1.id()));
        assertNotNull(svc.findById(p2.id()));
    }
} 
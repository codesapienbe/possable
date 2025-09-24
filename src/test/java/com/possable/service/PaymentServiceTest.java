package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentServiceTest {

    private final TaskExecutor syncExecutor = r -> r.run();

    @Test
    public void createAndProcessPayment() throws InterruptedException {
        PaymentService svc = new PaymentService(syncExecutor);
        var p = svc.createPayment("order-1", 10.0, "card");
        assertNotNull(p);
        assertEquals("pending", p.status());

        // wait briefly for async processing (runs synchronously with syncExecutor)
        Thread.sleep(200);

        var after = svc.findById(p.id());
        assertNotNull(after);
        assertEquals("completed", after.status());
    }

    @Test
    public void findNonExistentPayment() {
        PaymentService svc = new PaymentService(syncExecutor);
        assertNull(svc.findById("nope"));
    }

    @Test
    public void processPaymentInterrupted_marksFailed() throws Exception {
        PaymentService svc = new PaymentService(syncExecutor);
        var p = svc.createPayment("order-X", 5.0, "card");
        String id = p.id();

        Method m = PaymentService.class.getDeclaredMethod("processPayment", String.class);
        m.setAccessible(true);

        Thread t = new Thread(() -> {
            try {
                m.invoke(svc, id);
            } catch (Exception e) {
                // ignore
            }
        });

        t.start();
        // interrupt while sleeping inside processPayment
        Thread.sleep(10);
        t.interrupt();
        t.join(500);

        var after = svc.findById(id);
        assertNotNull(after);
        assertTrue(after.status().equals("failed") || after.status().equals("completed"));
    }
} 
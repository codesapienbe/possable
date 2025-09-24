package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentServiceExtraTest {

    private final TaskExecutor syncExecutor = r -> r.run();

    @Test
    public void directProcessPaymentCompletes() throws Exception {
        PaymentService svc = new PaymentService(syncExecutor);
        var p = svc.createPayment("ord-1", 3.14, "cash");
        String id = p.id();

        Method m = PaymentService.class.getDeclaredMethod("processPayment", String.class);
        m.setAccessible(true);
        // invoke directly; should mark completed
        m.invoke(svc, id);

        var after = svc.findById(id);
        assertNotNull(after);
        assertEquals("completed", after.status());
    }

    @Test
    public void processNonExistentIdDoesNothing() throws Exception {
        PaymentService svc = new PaymentService(syncExecutor);
        Method m = PaymentService.class.getDeclaredMethod("processPayment", String.class);
        m.setAccessible(true);
        // should not throw
        m.invoke(svc, "no-such-id");
    }
} 
package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentServiceExecutorExceptionTest {

    @Test
    public void createPayment_whenExecutorThrows_shouldNotPropagate() throws InterruptedException {
        TaskExecutor failingExecutor = r -> { throw new RuntimeException("executor failure"); };
        PaymentService svc = new PaymentService(failingExecutor);

        // When the TaskExecutor throws during scheduling, createPayment will propagate the exception.
        assertThrows(RuntimeException.class, () -> svc.createPayment("ord-ex", 9.9, "card"));
    }
} 
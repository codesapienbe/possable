package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceExecutorExceptionTest {

    @Test
    public void createOrder_whenExecutorThrows_shouldPropagate() {
        TaskExecutor failing = r -> { throw new RuntimeException("exec fail"); };
        OrderService svc = new OrderService(failing);
        assertThrows(RuntimeException.class, () -> svc.createOrder(List.of("x"), "notes"));
    }
} 
package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceExtraTest {

    private final TaskExecutor syncExecutor = r -> r.run();

    @Test
    public void listMultipleOrdersAndGetNotFound() {
        OrderService svc = new OrderService(syncExecutor);
        var o1 = svc.createOrder(List.of("a"), null);
        var o2 = svc.createOrder(List.of("b"), null);

        var all = svc.listOrders();
        assertTrue(all.size() >= 2);

        assertNull(svc.findById("nope"));
    }
} 
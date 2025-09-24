package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceTest {

    private final TaskExecutor syncExecutor = r -> r.run();

    @Test
    public void createListFindUpdateOrder() throws InterruptedException {
        OrderService svc = new OrderService(syncExecutor);

        var order = svc.createOrder(List.of("item1", "item2"), "notes");
        assertNotNull(order);
        assertNotNull(order.id());
        assertEquals("PENDING", order.status());
        assertEquals(2, order.items().size());

        var all = svc.listOrders();
        assertTrue(all.stream().anyMatch(o -> o.id().equals(order.id())));

        var found = svc.findById(order.id());
        assertNotNull(found);

        var updated = svc.updateStatus(order.id(), "COMPLETED");
        assertNotNull(updated);
        assertEquals("COMPLETED", updated.status());

        // verify update persisted
        var foundAfter = svc.findById(order.id());
        assertEquals("COMPLETED", foundAfter.status());
    }
} 
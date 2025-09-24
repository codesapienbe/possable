package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.lang.reflect.Method;
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

    @Test
    public void updateNonExistentReturnsNull() {
        OrderService svc = new OrderService(syncExecutor);
        var r = svc.updateStatus("nope", "X");
        assertNull(r);
    }

    @Test
    public void processOrderExceptionPath() throws Exception {
        OrderService svc = new OrderService(syncExecutor);
        var order = svc.createOrder(List.of("a"), "notes");

        Method m = OrderService.class.getDeclaredMethod("processOrder", com.possable.controller.OrderController.OrderDto.class, String.class);
        m.setAccessible(true);

        // invoke with null notes to run through process method (should not throw)
        m.invoke(svc, order, null);

        // ensure order exists after process
        assertNotNull(svc.findById(order.id()));
    }
} 
package com.possable.service;

import com.possable.controller.OrderController.OrderDto;
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
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final List<OrderDto> orders = Collections.synchronizedList(new ArrayList<>());
    private final TaskExecutor taskExecutor;

    public OrderService(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public OrderDto createOrder(List<String> items, String notes) {
        String id = UUID.randomUUID().toString();
        OrderDto order = new OrderDto(id, List.copyOf(items), "PENDING", Instant.now());
        orders.add(order);

        // Log in structured format
        log.info("{\"message\":\"order_created\", \"order_id\":\"{}\", \"items_count\":{}, \"component\":\"order-service\"}", id, items.size());

        // Simulate async printing/processing using virtual thread executor
        CompletableFuture.runAsync(() -> processOrder(order, notes), taskExecutor::execute)
                .exceptionally(ex -> {
                    log.error("order processing failed for {}", id, ex);
                    return null;
                });

        return order;
    }

    public List<OrderDto> listOrders() {
        synchronized (orders) {
            return List.copyOf(orders);
        }
    }

    public OrderDto findById(String id) {
        synchronized (orders) {
            return orders.stream().filter(o -> o.id().equals(id)).findFirst().orElse(null);
        }
    }

    public OrderDto updateStatus(String id, String status) {
        synchronized (orders) {
            for (int i = 0; i < orders.size(); i++) {
                var o = orders.get(i);
                if (o.id().equals(id)) {
                    OrderDto updated = new OrderDto(o.id(), o.items(), status, o.createdAt());
                    orders.set(i, updated);
                    log.info("{\"message\":\"order_status_updated\", \"order_id\":\"{}\", \"status\":\"{}\", \"component\":\"order-service\"}", id, status);
                    return updated;
                }
            }
            return null;
        }
    }

    private void processOrder(OrderDto order, String notes) {
        log.debug("processing order {}", order.id());
        try {
            // placeholder for actual printing/integration
            Thread.sleep(50);
            log.info("{\"message\":\"order_processed\", \"order_id\":\"{}\", \"component\":\"order-service\"}", order.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("order processing interrupted {}", order.id());
        }
    }
} 
package com.possable.order;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.possable.order.internal.OrderModuleService;

/**
 * Public API facade for the Order module.
 * This is the only class that other modules should depend on.
 */
@Service
public class OrderFacade {

    private final OrderModuleService orderModuleService;

    public record OrderInfo(String id, List<String> items, String status, Instant createdAt) {}

    public OrderFacade(OrderModuleService orderModuleService) {
        this.orderModuleService = orderModuleService;
    }

    public OrderInfo createOrder(List<String> items, String notes) {
        var dto = orderModuleService.createOrder(items, notes);
        return new OrderInfo(dto.id(), dto.items(), dto.status(), dto.createdAt());
    }

    public List<OrderInfo> listOrders() {
        return orderModuleService.listOrders().stream()
            .map(dto -> new OrderInfo(dto.id(), dto.items(), dto.status(), dto.createdAt()))
            .toList();
    }

    public OrderInfo findById(String id) {
        var dto = orderModuleService.findById(id);
        return dto != null ? new OrderInfo(dto.id(), dto.items(), dto.status(), dto.createdAt()) : null;
    }

    public OrderInfo updateStatus(String id, String status) {
        var dto = orderModuleService.updateStatus(id, status);
        return dto != null ? new OrderInfo(dto.id(), dto.items(), dto.status(), dto.createdAt()) : null;
    }

    public Map<String, Object> listOrdersPaged(Map<String, String> filters) {
        return orderModuleService.listOrdersPaged(filters);
    }
} 
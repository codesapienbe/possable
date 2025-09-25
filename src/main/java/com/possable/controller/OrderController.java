package com.possable.controller;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.OrderService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var order = orderService.createOrder(request.getItems(), request.getNotes());
        return ResponseEntity.created(URI.create("/orders/" + order.getId())).body(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> listOrders() {
        return ResponseEntity.ok(orderService.listOrders());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable String orderId) {
        var o = orderService.findById(orderId);
        if (o == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(o);
    }

    public static class UpdateOrderStatusRequest {
        @NotBlank
        private String status;
        public UpdateOrderStatusRequest() {}
        public UpdateOrderStatusRequest(String status) { this.status = status; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable String orderId, @Valid @RequestBody UpdateOrderStatusRequest req) {
        var updated = orderService.updateStatus(orderId, req.getStatus());
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    public static class CreateOrderRequest {
        @NotNull
        @NotEmpty
        private List<@NotNull String> items;
        private String notes;
        public CreateOrderRequest() {}
        public CreateOrderRequest(List<String> items, String notes) { this.items = items; this.notes = notes; }
        public List<String> getItems() { return items; }
        public void setItems(List<String> items) { this.items = items; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class OrderDto {
        private String id;
        private List<String> items;
        private String status;
        private Instant createdAt;
        public OrderDto() {}
        public OrderDto(String id, List<String> items, String status, Instant createdAt) { this.id = id; this.items = items; this.status = status; this.createdAt = createdAt; }
        public String getId() { return id; }
        public List<String> getItems() { return items; }
        public String getStatus() { return status; }
        public Instant getCreatedAt() { return createdAt; }
        // Backwards-compatible record-style accessors used across the codebase/tests
        public String id() { return getId(); }
        public List<String> items() { return getItems(); }
        public String status() { return getStatus(); }
        public Instant createdAt() { return getCreatedAt(); }
    }
} 
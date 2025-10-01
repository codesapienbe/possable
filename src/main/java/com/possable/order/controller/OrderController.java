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

import com.possable.order.OrderFacade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderFacade orderFacade;

    public OrderController(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var order = orderFacade.createOrder(request.getItems(), request.getNotes());
        var dto = new OrderDto(order.id(), order.items(), order.status(), order.createdAt());
        return ResponseEntity.created(URI.create("/orders/" + dto.id())).body(dto);
    }

    @GetMapping
    public ResponseEntity<java.util.Map<String,Object>> listOrders(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int limit) {
        var filters = new java.util.HashMap<String, String>();
        if (page != null) filters.put("page", Integer.toString(Math.max(0, page)));
        if (limit > 0) filters.put("limit", Integer.toString(Math.max(1, Math.min(100, limit))));
        return ResponseEntity.ok(orderFacade.listOrdersPaged(filters));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable String orderId) {
        var order = orderFacade.findById(orderId);
        if (order == null) return ResponseEntity.notFound().build();
        var dto = new OrderDto(order.id(), order.items(), order.status(), order.createdAt());
        return ResponseEntity.ok(dto);
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
        var order = orderFacade.updateStatus(orderId, req.getStatus());
        if (order == null) return ResponseEntity.notFound().build();
        var dto = new OrderDto(order.id(), order.items(), order.status(), order.createdAt());
        return ResponseEntity.ok(dto);
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

    public static record OrderDto(String id, List<String> items, String status, Instant createdAt) {
        // Backwards-compatible bean-style getters
        public String getId() { return id(); }
        public List<String> getItems() { return items(); }
        public String getStatus() { return status(); }
        public Instant getCreatedAt() { return createdAt(); }
    }
} 
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
        var order = orderService.createOrder(request.items(), request.notes());
        return ResponseEntity.created(URI.create("/orders/" + order.id())).body(order);
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

    public record UpdateOrderStatusRequest(@NotBlank String status) {}

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable String orderId, @Valid @RequestBody UpdateOrderStatusRequest req) {
        var updated = orderService.updateStatus(orderId, req.status());
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    public record CreateOrderRequest(@NotNull @NotEmpty List<@NotNull String> items, String notes) {}

    public record OrderDto(String id, List<String> items, String status, Instant createdAt) {}
} 
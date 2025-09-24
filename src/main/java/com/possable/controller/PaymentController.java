package com.possable.controller;

import com.possable.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public record CreatePaymentRequest(@NotBlank String orderId, @NotNull Double amount, @NotBlank String method) {}

    @PostMapping
    public ResponseEntity<PaymentService.Payment> createPayment(@Valid @RequestBody CreatePaymentRequest req) {
        var p = paymentService.createPayment(req.orderId(), req.amount(), req.method());
        return ResponseEntity.created(URI.create("/payments/" + p.id())).body(p);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentService.Payment> getPayment(@PathVariable String paymentId) {
        var p = paymentService.findById(paymentId);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }
} 
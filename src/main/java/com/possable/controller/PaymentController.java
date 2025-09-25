package com.possable.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.PaymentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public static class CreatePaymentRequest {
        @NotBlank
        private String orderId;
        @NotNull
        private Double amount;
        @NotBlank
        private String method;
        public CreatePaymentRequest() {}
        public CreatePaymentRequest(String orderId, Double amount, String method) { this.orderId = orderId; this.amount = amount; this.method = method; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
    }

    @PostMapping
    public ResponseEntity<PaymentService.Payment> createPayment(@Valid @RequestBody CreatePaymentRequest req) {
        var p = paymentService.createPayment(req.getOrderId(), req.getAmount(), req.getMethod());
        return ResponseEntity.created(URI.create("/payments/" + p.id())).body(p);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentService.Payment> getPayment(@PathVariable String paymentId) {
        var p = paymentService.findById(paymentId);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }
} 
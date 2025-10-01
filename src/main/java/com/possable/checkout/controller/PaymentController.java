package com.possable.controller;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.checkout.CheckoutFacade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final CheckoutFacade checkoutFacade;

    public PaymentController(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    public static class CreatePaymentRequest {
        @NotBlank
        private String orderId;
        @NotNull
        private Double amount;
        @NotBlank
        private String method;
        public CreatePaymentRequest() {}
        public CreatePaymentRequest(String orderId, Double amount, String method) { 
            this.orderId = orderId; 
            this.amount = amount; 
            this.method = method; 
        }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
    }

    public static record PaymentDto(String id, String orderId, double amount, String method, String status, Instant paidAt) {
        // Backwards-compatible bean-style getters
        public String getId() { return id(); }
        public String getOrderId() { return orderId(); }
        public double getAmount() { return amount(); }
        public String getMethod() { return method(); }
        public String getStatus() { return status(); }
        public Instant getPaidAt() { return paidAt(); }
    }

    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody CreatePaymentRequest req) {
        var payment = checkoutFacade.createPayment(req.getOrderId(), req.getAmount(), req.getMethod());
        var dto = new PaymentDto(payment.id(), payment.orderId(), payment.amount(), 
            payment.method(), payment.status(), payment.paidAt());
        return ResponseEntity.created(URI.create("/payments/" + dto.id())).body(dto);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable String paymentId) {
        var payment = checkoutFacade.findById(paymentId);
        if (payment == null) return ResponseEntity.notFound().build();
        var dto = new PaymentDto(payment.id(), payment.orderId(), payment.amount(), 
            payment.method(), payment.status(), payment.paidAt());
        return ResponseEntity.ok(dto);
    }
} 
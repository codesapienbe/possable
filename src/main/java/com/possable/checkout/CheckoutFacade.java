package com.possable.checkout;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.possable.checkout.internal.CheckoutModuleService;

/**
 * Public API facade for the Checkout module.
 * This is the only class that other modules should depend on.
 */
@Service
public class CheckoutFacade {

    private final CheckoutModuleService checkoutModuleService;

    public record PaymentInfo(String id, String orderId, double amount, String method, String status, Instant paidAt) {}

    public CheckoutFacade(CheckoutModuleService checkoutModuleService) {
        this.checkoutModuleService = checkoutModuleService;
    }

    public PaymentInfo createPayment(String orderId, double amount, String method) {
        var payment = checkoutModuleService.createPayment(orderId, amount, method);
        return new PaymentInfo(payment.id(), payment.orderId(), payment.amount(), payment.method(), payment.status(), payment.paidAt());
    }

    public PaymentInfo findById(String id) {
        var payment = checkoutModuleService.findById(id);
        return payment != null 
            ? new PaymentInfo(payment.id(), payment.orderId(), payment.amount(), payment.method(), payment.status(), payment.paidAt())
            : null;
    }

    public List<PaymentInfo> listPayments() {
        return checkoutModuleService.listPayments().stream()
            .map(p -> new PaymentInfo(p.id(), p.orderId(), p.amount(), p.method(), p.status(), p.paidAt()))
            .toList();
    }
} 
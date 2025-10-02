package com.possable.checkout.service;

import java.util.List;

import com.possable.checkout.CheckoutFacade;
import com.possable.checkout.CheckoutMainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "checkout/service", layout = CheckoutMainLayout.class)
@PageTitle("Checkout - Service")
@PreAuthorize("hasAnyRole('SERVICE','MANAGEMENT')")
public class CheckoutServiceView extends VerticalLayout {

    private final CheckoutFacade checkoutFacade;
    private final Grid<CheckoutFacade.PaymentInfo> grid = new Grid<>(CheckoutFacade.PaymentInfo.class, false);

    public CheckoutServiceView(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Service - Payment Requests"));
        add(new Span("Create payment requests for tables and view recent payments."));

        grid.addColumn(p -> p.id()).setHeader("Payment ID").setAutoWidth(true);
        grid.addColumn(p -> p.orderId()).setHeader("Order ID").setAutoWidth(true);
        grid.addColumn(p -> String.format("$%.2f", p.amount())).setHeader("Amount").setAutoWidth(true);
        grid.addColumn(p -> p.method()).setHeader("Method").setAutoWidth(true);
        grid.addColumn(p -> p.status()).setHeader("Status").setAutoWidth(true);

        add(grid);

        HorizontalLayout form = new HorizontalLayout();
        TextField orderId = new TextField("Order ID");
        NumberField amount = new NumberField("Amount");
        amount.setMin(0);
        ComboBox<String> method = new ComboBox<>("Method");
        method.setItems("cash", "card");
        method.setValue("card");
        Button reqPay = new Button("Request Payment", e -> {
            if (orderId.getValue() == null || orderId.getValue().isBlank()) { Notification.show("Order ID required"); return; }
            double a = amount.getValue() == null ? 0.0 : amount.getValue();
            try {
                var pi = checkoutFacade.createPayment(orderId.getValue(), a, method.getValue());
                Notification.show("Payment requested: " + pi.id());
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Failed to request payment");
            }
        });

        form.add(orderId, amount, method, reqPay);
        add(form);

        refreshGrid();
    }

    private void refreshGrid() {
        try {
            List<CheckoutFacade.PaymentInfo> items = checkoutFacade.listPayments();
            grid.setItems(items);
        } catch (Exception ex) {
            grid.setItems(List.of());
        }
    }
} 
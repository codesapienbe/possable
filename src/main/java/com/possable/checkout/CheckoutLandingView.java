package com.possable.checkout;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "checkout", layout = CheckoutMainLayout.class)
@PageTitle("Checkout")
public class CheckoutLandingView extends VerticalLayout {

    public CheckoutLandingView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Checkout Module"));

        VerticalLayout links = new VerticalLayout();
        try { Class.forName("com.possable.checkout.customer.CheckoutCustomerView"); links.add(new RouterLink("Customer Checkout", com.possable.checkout.customer.CheckoutCustomerView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.checkout.service.CheckoutServiceView"); links.add(new RouterLink("Service Checkout", com.possable.checkout.service.CheckoutServiceView.class)); } catch (ClassNotFoundException ignore) {}
        // cashier and management pages could be added similarly if present
        add(links);
    }
} 
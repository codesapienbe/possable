package com.possable.inventory.customer;

import java.util.ArrayList;
import java.util.List;

import com.possable.inventory.InventoryFacade;
import com.possable.inventory.InventoryMainLayout;
import com.possable.order.OrderFacade;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "inventory/customer", layout = InventoryMainLayout.class)
@PageTitle("Menu")
@PreAuthorize("permitAll()")
public class InventoryCustomerView extends VerticalLayout {

    private final InventoryFacade inventoryFacade;
    private final OrderFacade orderFacade;

    public InventoryCustomerView(InventoryFacade inventoryFacade, OrderFacade orderFacade) {
        this.inventoryFacade = inventoryFacade;
        this.orderFacade = orderFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Customer Menu"));

        VerticalLayout itemsContainer = new VerticalLayout();
        itemsContainer.setSpacing(true);

        // session-backed cart (list of item ids) - make cart final for lambda capture
        com.vaadin.flow.server.VaadinSession vs = com.vaadin.flow.server.VaadinSession.getCurrent();
        @SuppressWarnings("unchecked")
        List<String> existing = vs == null ? null : (List<String>) vs.getAttribute("customer_cart");
        final List<String> cart = new ArrayList<>();
        if (existing != null) cart.addAll(existing);
        if (vs != null) vs.setAttribute("customer_cart", cart);

        // helper to render cart summary
        HorizontalLayout cartRow = new HorizontalLayout();
        cartRow.setSpacing(true);
        Span cartLabel = new Span("Cart: " + cart.size() + " items");
        Button placeOrder = new Button("Place Order", e -> {
            if (cart.isEmpty()) { Notification.show("Cart is empty"); return; }
            var created = orderFacade.createOrder(new ArrayList<>(cart), "Order from customer");
            cart.clear();
            if (vs != null) vs.setAttribute("customer_cart", cart);
            cartLabel.setText("Cart: 0 items");
            Notification.show("Order created: " + created.id(), 3000, Notification.Position.TOP_CENTER);
        });
        cartRow.add(cartLabel, placeOrder);

        add(cartRow);

        // load items and render
        var items = inventoryFacade.listItems(200);
        for (var it : items) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
            Span name = new Span(it.name() + " — " + String.format("$%.2f", it.price()));
            Button add = new Button("Add", ev -> {
                cart.add(it.id());
                if (vs != null) vs.setAttribute("customer_cart", cart);
                cartLabel.setText("Cart: " + cart.size() + " items");
            });
            row.add(name, add);
            itemsContainer.add(row);
        }

        add(itemsContainer);
    }
} 
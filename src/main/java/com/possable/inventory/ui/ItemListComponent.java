package com.possable.inventory.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.possable.inventory.InventoryFacade;
import com.possable.order.OrderFacade;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

/**
 * Vaadin component for displaying menu items and cart.
 * Uses InventoryFacade and OrderFacade for module-based access.
 */
public class ItemListComponent extends VerticalLayout {

    public ItemListComponent(InventoryFacade inventoryFacade, OrderFacade orderFacade) {
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        HorizontalLayout main = new HorizontalLayout();
        main.setWidthFull();
        main.setSpacing(true);

        HorizontalLayout tiles = new HorizontalLayout();
        tiles.setWidthFull();
        tiles.getStyle().set("flex-wrap", "wrap").set("gap", "12px");

        // simple in-memory cart: itemId -> [item, count]
        Map<String, CartEntry> cart = new LinkedHashMap<>();

        // cart view on the right (declare early so lambdas can close over it)
        VerticalLayout cartView = new VerticalLayout();
        cartView.setWidth("320px");
        cartView.addClassName("pos-cart");
        cartView.setPadding(true);

        Span cartTitle = new Span("Cart");
        cartTitle.getElement().getThemeList().add("badge");
        cartView.add(cartTitle);

        Div itemsContainer = new Div();
        itemsContainer.setWidthFull();
        cartView.add(itemsContainer);

        Span totalSpan = new Span("Total: $0.00");
        totalSpan.getElement().getStyle().set("font-weight", "bold");
        cartView.add(totalSpan);

        TextArea notes = new TextArea("Notes");
        notes.setWidthFull();
        cartView.add(notes);

        List<InventoryFacade.ItemInfo> items = inventoryFacade.listItems(200);
        for (var it : items) {
            Button b = new Button(it.name() + "\n" + String.format("$%.2f", it.price()));
            b.addClassNames("pos-tile", "pos-tile-large");
            b.getElement().setAttribute("aria-label", it.name());
            b.addClickListener(evt -> {
                addToCart(cart, it);
                refreshCartView(itemsContainer, totalSpan, cart);
            });
            tiles.add(b);
        }

        Button placeOrder = new Button("Place Order", evt -> {
            if (cart.isEmpty()) {
                Notification.show("Cart is empty");
                return;
            }
            // build list of item ids (repeat per quantity)
            List<String> payload = new ArrayList<>();
            for (var e : cart.values()) {
                for (int i = 0; i < e.count; i++) payload.add(e.item.id());
            }
            var created = orderFacade.createOrder(payload, notes.getValue());
            Notification.show("Order placed: " + created.id());
            cart.clear();
            notes.clear();
            refreshCartView(itemsContainer, totalSpan, cart);
        });
        placeOrder.addClassName("pos-button-primary");
        cartView.add(placeOrder);

        main.add(tiles, cartView);
        main.setFlexGrow(1, tiles);
        main.setFlexGrow(0, cartView);
        add(main);
    }

    private static class CartEntry {
        final InventoryFacade.ItemInfo item;
        int count;
        CartEntry(InventoryFacade.ItemInfo item) { this.item = item; this.count = 0; }
    }

    private void addToCart(Map<String, CartEntry> cart, InventoryFacade.ItemInfo item) {
        CartEntry e = cart.computeIfAbsent(item.id(), k -> new CartEntry(item));
        e.count++;
    }

    private void refreshCartView(Div itemsContainer, Span totalSpan, Map<String, CartEntry> cart) {
        itemsContainer.removeAll();
        double total = 0.0;
        for (var e : cart.values()) {
            Div line = new Div();
            line.setText(e.item.name() + " x " + e.count + " - " + String.format("$%.2f", e.item.price() * e.count));
            Button remove = new Button("-", ev -> { 
                if (e.count > 0) e.count--; 
                if (e.count == 0) cart.remove(e.item.id()); 
                refreshCartView(itemsContainer, totalSpan, cart); 
            });
            remove.getStyle().set("margin-left", "8px");
            line.add(remove);
            itemsContainer.add(line);
            total += e.item.price() * e.count;
        }
        totalSpan.setText("Total: " + String.format("$%.2f", total));
    }
} 

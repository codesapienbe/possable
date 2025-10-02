package com.possable.menu;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.possable.infrastructure.ui.MainLayout;
import com.possable.inventory.InventoryFacade;
import com.possable.order.OrderFacade;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "menu", layout = MainLayout.class)
@PageTitle("Menu")
public class MenuView extends VerticalLayout {

    private final InventoryFacade inventoryFacade;
    private final OrderFacade orderFacade;
    private final DecimalFormat priceFormat = new DecimalFormat("$#.00");

    public MenuView(InventoryFacade inventoryFacade, OrderFacade orderFacade) {
        this.inventoryFacade = inventoryFacade;
        this.orderFacade = orderFacade;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Menu"));

        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setSpacing(true);

        // Category tabs
        Button starterTab = new Button("Starter");
        Button mainTab = new Button("Main Course");
        Button drinksTab = new Button("Drinks");
        Button dessertsTab = new Button("Desserts");
        starterTab.addClassName("pos-button-large");
        mainTab.addClassName("pos-button-large");
        drinksTab.addClassName("pos-button-large");
        dessertsTab.addClassName("pos-button-large");

        topRow.add(starterTab, mainTab, drinksTab, dessertsTab);
        add(topRow);

        HorizontalLayout main = new HorizontalLayout();
        main.setWidthFull();
        main.setSpacing(true);

        // Grid of cards
        Div grid = new Div();
        grid.getStyle().set("display", "flex");
        grid.getStyle().set("flex-wrap", "wrap");
        grid.getStyle().set("gap", "14px");
        grid.setWidthFull();

        // Simple cart: itemId -> CartEntry
        Map<String, CartEntry> cart = new LinkedHashMap<>();

        // Right-side order summary
        VerticalLayout cartView = new VerticalLayout();
        cartView.setWidth("360px");
        cartView.addClassName("pos-cart");
        Span cartTitle = new Span("Order");
        cartTitle.getElement().getThemeList().add("badge");
        cartView.add(cartTitle);
        Div itemsContainer = new Div();
        itemsContainer.setWidthFull();
        cartView.add(itemsContainer);

        Span totalSpan = new Span("Total: $0.00");
        totalSpan.getStyle().set("font-weight", "800");
        cartView.add(totalSpan);

        Button sendOrder = new Button("Send Order", e -> {
            if (cart.isEmpty()) {
                return;
            }
            List<String> payload = new ArrayList<>();
            for (var ce : cart.values()) {
                for (int i = 0; i < ce.quantity; i++) payload.add(ce.item.id());
            }
            var created = orderFacade.createOrder(payload, "Order from menu");
            // navigate to orders page after placing order
            getUI().ifPresent(ui -> ui.navigate("orders"));
        });
        sendOrder.addClassName("pos-button-large");
        cartView.add(sendOrder);

        main.add(grid, cartView);
        main.setFlexGrow(1, grid);
        main.setFlexGrow(0, cartView);
        add(main);

        // helper to refresh cart view
        Runnable refreshCart = () -> {
            itemsContainer.removeAll();
            double total = 0.0;
            for (CartEntry e : cart.values()) {
                Div line = new Div();
                line.getStyle().set("display", "flex");
                line.getStyle().set("justify-content", "space-between");
                line.getStyle().set("align-items", "center");
                line.setText(e.item.name() + " x " + e.quantity + "  " + priceFormat.format(e.item.price() * e.quantity));
                Button remove = new Button("-", ev -> {
                    if (e.quantity > 0) e.quantity--;
                    if (e.quantity == 0) cart.remove(e.item.id());
                    refreshCart.run();
                });
                remove.getStyle().set("margin-left", "8px");
                line.add(remove);
                itemsContainer.add(line);
                total += e.item.price() * e.quantity;
            }
            totalSpan.setText("Total: " + priceFormat.format(total));
        };

        // wiring category tab clicks to load grid
        starterTab.addClickListener(e -> loadCategoryGrid("starter", grid, cart, refreshCart));
        mainTab.addClickListener(e -> loadCategoryGrid("main", grid, cart, refreshCart));
        drinksTab.addClickListener(e -> loadCategoryGrid("drinks", grid, cart, refreshCart));
        dessertsTab.addClickListener(e -> loadCategoryGrid("dessert", grid, cart, refreshCart));

        // initial load
        loadCategoryGrid("starter", grid, cart, refreshCart);
    }

    private static class CartEntry {
        final InventoryFacade.ItemInfo item;
        int quantity;
        CartEntry(InventoryFacade.ItemInfo item) { this.item = item; this.quantity = 0; }
    }

    private void loadCategoryGrid(String category, Div grid, Map<String, CartEntry> cart, Runnable refreshCart) {
        grid.removeAll();
        List<InventoryFacade.ItemInfo> items = inventoryFacade.listItems(1000).stream()
            .filter(i -> category.equalsIgnoreCase(i.category() == null ? "" : i.category()))
            .collect(Collectors.toList());

        for (var it : items) {
            Div card = new Div();
            card.addClassName("menu-item-card");
            // image placeholder
            Div image = new Div();
            image.addClassName("menu-item-image");
            // optional: use tags or name to derive background color or image
            card.add(image);

            Div body = new Div();
            body.addClassName("menu-item-body");
            Label title = new Label(it.name());
            title.getStyle().set("font-weight", "800").set("display", "block").set("margin-bottom", "8px");
            Label price = new Label(priceFormat.format(it.price()));
            price.getStyle().set("font-weight", "800").set("float", "right");
            body.add(title, price);

            // tags
            Div tags = new Div();
            tags.getStyle().set("margin-top", "8px");
            if (it.tagsCsv() != null && !it.tagsCsv().isBlank()) {
                for (String t : it.tagsCsv().split(",")) {
                    Span badge = new Span(t.trim());
                    badge.addClassName("menu-tag");
                    tags.add(badge);
                }
            }
            body.add(tags);

            Button add = new Button("Add", ev -> {
                CartEntry e = cart.computeIfAbsent(it.id(), k -> new CartEntry(it));
                e.quantity++;
                refreshCart.run();
            });
            add.addClassName("pos-button-large");
            body.add(add);

            card.add(body);
            grid.add(card);
        }
    }
} 
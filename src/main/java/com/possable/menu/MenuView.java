package com.possable.menu;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.possable.infrastructure.ui.MainLayout;
import com.possable.order.OrderFacade;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.fasterxml.jackson.databind.ObjectMapper;

@Route(value = "menu", layout = MainLayout.class)
@PageTitle("Menu")
public class MenuView extends VerticalLayout {

    private final OrderFacade orderFacade;
    private final DecimalFormat priceFormat = new DecimalFormat("$#.00");
    private Div currentGrid;
    private final Map<String, CartEntry> cart = new LinkedHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private Runnable refreshCart;

    public MenuView(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("HOME PAGE");
        header.getStyle().set("color", "#1f2937"); // dark slate for better contrast
        header.getStyle().set("margin", "0 0 12px 0");
        add(header);

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

        // Simple cart: itemId -> CartEntry (now a field)

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
                for (int i = 0; i < ce.quantity; i++) payload.add(ce.item.id);
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

        // helper to refresh cart
        this.refreshCart = () -> {
            itemsContainer.removeAll();
            double total = 0.0;
            for (CartEntry e : cart.values()) {
                Div line = new Div();
                line.getStyle().set("display", "flex");
                line.getStyle().set("justify-content", "space-between");
                line.getStyle().set("align-items", "center");
                line.setText(e.item.name + " x " + e.quantity + "  " + priceFormat.format(e.item.price * e.quantity));
                Button remove = new Button("-", ev -> {
                    if (e.quantity > 0) e.quantity--;
                    if (e.quantity == 0) cart.remove(e.item.id);
                    this.refreshCart.run();
                });
                remove.getStyle().set("margin-left", "8px");
                line.add(remove);
                itemsContainer.add(line);
                total += e.item.price * e.quantity;
            }
            totalSpan.setText("Total: " + priceFormat.format(total));
        };

        // wiring category tab clicks to load grid via browser fetch
        starterTab.addClickListener(e -> loadCategoryGridClient("starter", grid, cart, refreshCart));
        mainTab.addClickListener(e -> loadCategoryGridClient("main", grid, cart, refreshCart));
        drinksTab.addClickListener(e -> loadCategoryGridClient("drinks", grid, cart, refreshCart));
        dessertsTab.addClickListener(e -> loadCategoryGridClient("dessert", grid, cart, refreshCart));

        // initial load
        loadCategoryGridClient("starter", grid, cart, refreshCart);
    }

    private static class CartEntry {
        final MenuItem item;
        int quantity;
        CartEntry(MenuItem item) { this.item = item; this.quantity = 0; }
    }

    private static class MenuItem {
        final String id;
        final String name;
        final String description;
        final double price;
        final String category;
        final String tagsCsv;
        MenuItem(String id, String name, String description, double price, String category, String tagsCsv) {
            this.id = id; this.name = name; this.description = description; this.price = price; this.category = category; this.tagsCsv = tagsCsv;
        }
    }

    private void loadCategoryGridClient(String category, Div grid, Map<String, CartEntry> cart, Runnable refreshCart) {
        grid.removeAll();
        this.currentGrid = grid;
        try {
            String encoded = java.net.URLEncoder.encode(category, "UTF-8");
            // fetch on client and forward JSON string to server-side receiveItems
            getElement().executeJs("fetch('/items?limit=100&category=' + $1).then(r => r.json()).then(j => $0.$server.receiveItems(JSON.stringify(j))).catch(e => { console.warn(e); $0.$server.receiveItems('null'); });", getElement(), encoded);
        } catch (Exception ex) {
            // fallback: do nothing
        }
    }

    @ClientCallable
    public void receiveItems(String json) {
        if (json == null || json.isBlank() || json.equals("null")) return;
        try {
            Map<?,?> resp = mapper.readValue(json, Map.class);
            Object itemsObj = resp.get("items");
            @SuppressWarnings("unchecked")
            List<Map<String,Object>> items = itemsObj instanceof List ? (List<Map<String,Object>>) itemsObj : List.of();
            // render on UI thread
            UI ui = UI.getCurrent();
            if (ui == null) return;
            ui.access(() -> {
                currentGrid.removeAll();
                for (Map<String,Object> it : items) {
                    String id = String.valueOf(it.get("id"));
                    String name = String.valueOf(it.getOrDefault("name", ""));
                    String desc = String.valueOf(it.getOrDefault("description", ""));
                    double price = 0.0;
                    try { Object p = it.get("price"); price = p == null ? 0.0 : Double.parseDouble(String.valueOf(p)); } catch (Exception ignore) {}
                    String cat = String.valueOf(it.getOrDefault("category", ""));
                    String tags = String.valueOf(it.getOrDefault("tagsCsv", ""));

                    MenuItem menuItem = new MenuItem(id, name, desc, price, cat, tags);

                    Div card = new Div();
                    card.addClassName("menu-item-card");
                    Div image = new Div();
                    image.addClassName("menu-item-image");
                    card.add(image);

                    Div body = new Div();
                    body.addClassName("menu-item-body");
                    Span title = new Span(menuItem.name);
                    title.getStyle().set("font-weight", "800").set("display", "block").set("margin-bottom", "8px").set("color", "#0f172a");
                    Span priceLbl = new Span(priceFormat.format(menuItem.price));
                    priceLbl.getStyle().set("font-weight", "800").set("float", "right").set("color", "#059669");
                    body.add(title, priceLbl);

                    Div tagsDiv = new Div();
                    tagsDiv.getStyle().set("margin-top", "8px");
                    if (menuItem.tagsCsv != null && !menuItem.tagsCsv.isBlank()) {
                        for (String t : menuItem.tagsCsv.split(",")) {
                            Span badge = new Span(t.trim());
                            badge.addClassName("menu-tag");
                            // subtle badge styling for visual alignment
                            badge.getStyle().set("background-color", "#eef2ff");
                            badge.getStyle().set("color", "#1e293b");
                            badge.getStyle().set("padding", "2px 6px");
                            badge.getStyle().set("border-radius", "6px");
                            tagsDiv.add(badge);
                        }
                    }
                    body.add(tagsDiv);

                    Button add = new Button();
                    add.setIcon(new Icon(VaadinIcon.PLUS));
                    add.addClickListener(ev -> {
                        CartEntry e = cart.computeIfAbsent(menuItem.id, k -> new CartEntry(menuItem));
                        e.quantity++;
                        // preserve existing behavior: cart UI refresh handled elsewhere
                    });
                    add.addClassName("pos-button-large");
                    add.getElement().setAttribute("aria-label", "Add item");
                    body.add(add);

                    card.add(body);
                    currentGrid.add(card);
                }
            });
        } catch (Exception ex) {
            // ignore parse errors
        }
    }
} 
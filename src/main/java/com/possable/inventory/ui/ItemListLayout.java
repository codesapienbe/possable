package com.possable.inventory.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.possable.order.OrderFacade;
import com.possable.inventory.InventoryFacade;
import com.possable.print.PrintFacade;
import com.possable.order.ui.OrderView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

public class ItemListLayout extends VerticalLayout {

	public static record ItemDto(String id, String name, BigDecimal price, String category) {}

	private final VerticalLayout tiles = new VerticalLayout();
	private final com.vaadin.flow.component.orderedlayout.HorizontalLayout skeletonTiles = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
	private final VerticalLayout cartPanel = new VerticalLayout();
	private final List<ItemDto> cartItems = new ArrayList<>();
	private final Span cartCount = new Span("0 items");
	private final Span cartTotal = new Span("$0.00");
	private final OrderFacade orderFacade;
	private final PrintFacade printFacade;
	private final InventoryFacade inventoryFacade;
	private ComboBox<String> categoryFilter;
	private List<ItemDto> currentItems = List.of();

	public ItemListLayout(InventoryFacade inventoryFacade, OrderFacade orderFacade, PrintFacade printFacade) {
		this.orderFacade = orderFacade;
		this.printFacade = printFacade;
		this.inventoryFacade = inventoryFacade;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		H1 header = new H1("Menu");
		header.getStyle().set("font-size", "1.75em").set("margin", "0 0 8px 0");
		add(header);

		categoryFilter = new ComboBox<>("Category");
		categoryFilter.setPlaceholder("All categories");
		categoryFilter.addValueChangeListener(e -> renderTiles(currentItems, e.getValue()));
		categoryFilter.addClassName("pos-filter");
		add(categoryFilter);

		HorizontalLayout main = new HorizontalLayout();
		main.setWidthFull();
		main.getStyle().set("gap", "16px");

		tiles.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "12px");
		tiles.addClassName("pos-tiles");
		// skeleton tiles initially hidden
		skeletonTiles.setClassName("skeleton-tiles-container");
		for (int i = 0; i < 6; i++) {
			com.vaadin.flow.component.html.Div s = new com.vaadin.flow.component.html.Div();
			s.addClassName("skeleton-tile");
			skeletonTiles.add(s);
		}
		add(skeletonTiles);

		cartPanel.getStyle().set("min-width", "320px").set("border", "1px solid var(--lumo-contrast-10pct)").set("padding", "var(--lumo-space-m)");
		cartPanel.addClassName("pos-cart");
		cartPanel.add(new H1("Cart"));
		cartPanel.add(cartCount, cartTotal);

		TextArea notes = new TextArea("Notes");
		notes.setWidthFull();

		Button sendOrder = new Button("Place Order", evt -> sendOrderAndCreatePrintJobs(cartItems, notes.getValue()));
		sendOrder.getElement().getThemeList().add("primary");
		sendOrder.addClassName("pos-button-large");
		sendOrder.setIcon(new Icon(VaadinIcon.CART));

		Button clear = new Button("Clear", evt -> {
			cartItems.clear();
			refreshCart();
		});
		clear.addClassName("pos-button-large");

		cartPanel.add(notes, new HorizontalLayout(sendOrder, clear));

		main.add(tiles, cartPanel);
		main.setFlexGrow(3, tiles);
		main.setFlexGrow(1, cartPanel);

		add(main);

		// initial render will happen via navigation events; keep attach fallback
		addAttachListener(evt -> {
			if (currentItems.isEmpty()) reloadMenu();
		});
	}

	@SuppressWarnings("unchecked")
	private void reloadMenu() {
		// show skeletons
		skeletonTiles.setVisible(true);
		tiles.setVisible(false);
		List<InventoryFacade.ItemInfo> items = (List<InventoryFacade.ItemInfo>) inventoryFacade.listItemsPaged(Map.of("limit", "200")).get("items");
		currentItems = mapItems(items);
		categoryFilter.setItems(currentItems.stream().map(ItemDto::category).distinct().collect(Collectors.toList()));
		renderTiles(currentItems, categoryFilter.getValue());
		// hide skeletons
		skeletonTiles.setVisible(false);
		tiles.setVisible(true);
	}

	private void renderTiles(List<ItemDto> items, String category) {
		tiles.removeAll();
		List<ItemDto> filtered = items.stream().filter(i -> category == null || category.isBlank() || category.equals(i.category())).toList();
		if (filtered.isEmpty()) {
			com.vaadin.flow.component.html.Div empty = new com.vaadin.flow.component.html.Div();
			empty.addClassName("empty-state");
			empty.setText("No menu items available");
			tiles.add(empty);
			return;
		}
		for (ItemDto it : filtered) {
			com.vaadin.flow.component.html.Div tile = new com.vaadin.flow.component.html.Div();
			tile.addClassName("pos-tile"); tile.addClassName("pos-tile-large");
			tile.getElement().setAttribute("aria-label", it.name());
			tile.getElement().setAttribute("role", "button");
			tile.getElement().setAttribute("tabindex", "0");
			com.vaadin.flow.component.html.Span title = new com.vaadin.flow.component.html.Span(it.name());
			title.addClassName("tile-title");
			com.vaadin.flow.component.html.Span price = new com.vaadin.flow.component.html.Span(String.format("$%.2f", it.price().doubleValue()));
			price.getStyle().set("font-weight", "700");
			com.vaadin.flow.component.html.Span cat = new com.vaadin.flow.component.html.Span(it.category());
			cat.addClassName("pos-role-badge");
			tile.add(title, price, cat);
			// mouse click
			tile.addClickListener(evt -> { addToCart(it); Notification.show("Added to cart: " + it.name()); });
			// keyboard activation (Enter or Space) — forward to click via client-side listener
			tile.getElement().executeJs("this.addEventListener('keydown', function(e){ if(e.key==='Enter' || e.key===' '){ this.click(); e.preventDefault(); } });");
			tiles.add(tile);
		}
	}

	private void addToCart(ItemDto item) {
		cartItems.add(item);
		refreshCart();
	}

	private void refreshCart() {
		cartPanel.removeAll();
		cartPanel.add(new H1("Cart"));
		cartCount.setText(cartItems.size() + " items");
		BigDecimal total = cartItems.stream().map(ItemDto::price).reduce(BigDecimal.ZERO, BigDecimal::add);
		cartTotal.setText("$" + total.toString());
		cartTotal.getStyle().set("font-size", "1.25em").set("font-weight", "700");
		cartPanel.add(cartCount, cartTotal);
		for (int i = 0; i < cartItems.size(); i++) {
			ItemDto it = cartItems.get(i);
			HorizontalLayout row = new HorizontalLayout();
			row.setWidthFull();
			row.add(new Span(it.name() + " - $" + it.price().toString()));
			Button remove = new Button("Remove", e -> {
				cartItems.remove(it);
				refreshCart();
			});
			remove.addClassName("pos-button-large");
			row.add(remove);
			cartPanel.add(row);
		}
		TextArea notes = new TextArea("Notes");
		notes.setWidthFull();
		Button sendOrder = new Button("Place Order", evt -> sendOrderAndCreatePrintJobs(cartItems, notes.getValue()));
		sendOrder.getElement().getThemeList().add("primary");
		sendOrder.addClassName("pos-button-large");
		Button clear = new Button("Clear", evt -> {
			cartItems.clear();
			refreshCart();
		});
		clear.addClassName("pos-button-large");
		cartPanel.add(notes, new HorizontalLayout(sendOrder, clear));
	}

	private void sendOrderAndCreatePrintJobs(List<ItemDto> items, String notes) {
		if (items == null || items.isEmpty()) {
			Notification.show("Cart is empty");
			return;
		}
		try {
			var createdOrder = orderFacade.createOrder(items.stream().map(ItemDto::id).collect(Collectors.toList()), notes);
			// create print jobs for printers that have matching templates
			var templates = printFacade.listTemplates(Map.of());
			int created = 0;
			var missing = new StringBuilder();
			for (var p : printFacade.listPrinters(Map.of())) {
				var tpl = templates.stream().filter(t -> t.printerCategory().equals(p.category())).findFirst();
				if (tpl.isPresent()) {
					printFacade.createJob(createdOrder.id(), p.id(), tpl.get().id());
					created++;
				} else {
					if (missing.length() > 0) missing.append(", ");
					missing.append(p.name());
				}
			}
			if (created > 0) {
				orderFacade.updateStatus(createdOrder.id(), "IN_PREPARATION");
			}
			String msg = "Order sent (" + items.size() + " items) - print jobs created: " + created;
			if (missing.length() > 0) msg += "; no template for: " + missing.toString();
			Notification.show(msg);
			items.clear();
			refreshCart();
			UI.getCurrent().navigate(OrderView.class);
		} catch (Exception ex) {
			Notification.show("Failed to send order: " + ex.getMessage());
		}
	}

	private List<ItemDto> mapItems(List<InventoryFacade.ItemInfo> items) {
		return items.stream()
			.map(i -> new ItemDto(i.id(), i.name(), BigDecimal.valueOf(i.price()), i.description()))
			.toList();
	}
} 

package com.possable.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.possable.service.ItemService;
import com.possable.service.OrderService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "items", layout = MainLayout.class)
@PageTitle("Items")
public class ItemListView extends VerticalLayout {

	public static record ItemDto(String id, String name, BigDecimal price, String category) {}

	private final VerticalLayout tiles = new VerticalLayout();
	private final VerticalLayout cartPanel = new VerticalLayout();
	private final List<ItemDto> cartItems = new ArrayList<>();
	private final Span cartCount = new Span("0 items");
	private final Span cartTotal = new Span("$0.00");
	private final OrderService orderService;

	public ItemListView(ItemService itemService, OrderService orderService) {
		this.orderService = orderService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Menu"));

		ComboBox<String> categoryFilter = new ComboBox<>("Category");
		var items = mapItems(itemService.listItems(200));
		categoryFilter.setItems(items.stream().map(ItemDto::category).distinct().collect(Collectors.toList()));
		categoryFilter.addValueChangeListener(e -> renderTiles(items, e.getValue()));
		add(categoryFilter);

		HorizontalLayout main = new HorizontalLayout();
		main.setWidthFull();
		main.getStyle().set("gap", "16px");

		tiles.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "12px");
		tiles.addClassName("pos-tiles");
		renderTiles(items, null);

		cartPanel.getStyle().set("min-width", "320px").set("border", "1px solid var(--lumo-contrast-10pct)").set("padding", "var(--lumo-space-m)");
		cartPanel.addClassName("pos-cart");
		cartPanel.add(new H1("Cart"));
		cartPanel.add(cartCount, cartTotal);

		TextArea notes = new TextArea("Notes");
		notes.setWidthFull();

		Button sendOrder = new Button("Send Order", evt -> {
			if (cartItems.isEmpty()) {
				Notification.show("Cart is empty");
				return;
			}
			try {
				orderService.createOrder(cartItems.stream().map(ItemDto::id).collect(Collectors.toList()), notes.getValue());
				Notification.show("Order sent (" + cartItems.size() + " items)");
				cartItems.clear();
				refreshCart();
				UI.getCurrent().navigate(OrderView.class);
			} catch (Exception ex) {
				Notification.show("Failed to send order: " + ex.getMessage());
			}
		});

		Button clear = new Button("Clear", evt -> {
			cartItems.clear();
			refreshCart();
		});

		cartPanel.add(notes, new HorizontalLayout(sendOrder, clear));

		main.add(tiles, cartPanel);
		main.setFlexGrow(3, tiles);
		main.setFlexGrow(1, cartPanel);

		add(main);
	}

	private void renderTiles(List<ItemDto> items, String category) {
		tiles.removeAll();
		items.stream().filter(i -> category == null || category.isBlank() || category.equals(i.category()))
			.forEach(i -> {
				Button b = new Button(i.name() + "\n" + i.price().toString());
				b.getStyle().set("width", "200px").set("height", "90px").set("white-space", "pre-wrap");
				b.addClickListener(evt -> {
					addToCart(i);
					Notification.show("Added to cart: " + i.name());
				});
				tiles.add(b);
			});
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
			row.add(remove);
			cartPanel.add(row);
		}
		TextArea notes = new TextArea("Notes");
		notes.setWidthFull();
		Button sendOrder = new Button("Send Order", evt -> {
			if (cartItems.isEmpty()) {
				Notification.show("Cart is empty");
				return;
			}
			try {
				orderService.createOrder(cartItems.stream().map(ItemDto::id).collect(Collectors.toList()), notes.getValue());
				Notification.show("Order sent (" + cartItems.size() + " items)");
				cartItems.clear();
				refreshCart();
				UI.getCurrent().navigate(OrderView.class);
			} catch (Exception ex) {
				Notification.show("Failed to send order: " + ex.getMessage());
			}
		});
		Button clear = new Button("Clear", evt -> {
			cartItems.clear();
			refreshCart();
		});
		cartPanel.add(notes, new HorizontalLayout(sendOrder, clear));
	}

	private List<ItemDto> mapItems(List<ItemService.Item> items) {
		return items.stream()
			.map(i -> new ItemDto(i.id(), i.name(), BigDecimal.valueOf(i.price()), i.description()))
			.toList();
	}
} 
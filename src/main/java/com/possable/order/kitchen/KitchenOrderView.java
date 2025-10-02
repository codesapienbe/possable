package com.possable.order.kitchen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.possable.inventory.InventoryFacade;
import com.possable.order.OrderFacade;
import com.possable.order.OrderFacade.OrderInfo;
import com.possable.order.OrderMainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "orders/kitchen", layout = OrderMainLayout.class)
@PageTitle("Kitchen Orders")
@PreAuthorize("hasAnyRole('KITCHEN','MANAGEMENT')")
public class KitchenOrderView extends VerticalLayout {

	private final OrderFacade orderFacade;
	private final InventoryFacade inventoryFacade;

	public KitchenOrderView(OrderFacade orderFacade, InventoryFacade inventoryFacade) {
		this.orderFacade = orderFacade;
		this.inventoryFacade = inventoryFacade;
		setPadding(true);
		setSpacing(true);
		setWidthFull();

		add(new H1("Kitchen - Next Orders"));

		Button refresh = new Button("Refresh", evt -> render());
		add(refresh);

		render();
	}

	private void render() {
		removeAll();
		add(new H1("Kitchen - Next Orders"));
		Button refresh = new Button("Refresh", evt -> render());
		add(refresh);

		List<OrderInfo> orders = orderFacade.listOrders().stream()
				.filter(o -> o.status() != null && (o.status().equalsIgnoreCase("PENDING") || o.status().equalsIgnoreCase("IN_PREPARATION")))
				.limit(10)
				.collect(Collectors.toList());

		if (orders.isEmpty()) {
			add(new Span("No outstanding orders"));
			return;
		}

		// aggregate item counts by category and by item name and track contributing orders
		Map<String, Integer> categoryCounts = new HashMap<>();
		Map<String, Integer> itemCounts = new HashMap<>();
		Map<String, List<String>> itemToOrders = new HashMap<>();

		for (OrderInfo o : orders) {
			if (o.items() == null) continue;
			for (String itemId : o.items()) {
				try {
					var it = inventoryFacade.findById(itemId);
					String cat = it == null ? "uncategorized" : (it.category() == null ? "uncategorized" : it.category());
					String name = it == null ? itemId : (it.name() == null ? itemId : it.name());
					categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
					itemCounts.put(name, itemCounts.getOrDefault(name, 0) + 1);
					itemToOrders.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(o.id());
				} catch (Exception ignore) {
					itemCounts.put(itemId, itemCounts.getOrDefault(itemId, 0) + 1);
					categoryCounts.put("uncategorized", categoryCounts.getOrDefault("uncategorized", 0) + 1);
					itemToOrders.computeIfAbsent(itemId, k -> new java.util.ArrayList<>()).add(o.id());
				}
			}
		}

		// render aggregation with improved visuals
		VerticalLayout agg = new VerticalLayout();
		agg.getStyle().set("padding", "10px");
		Span title = new Span("Aggregated items across next " + orders.size() + " orders");
		title.getStyle().set("font-weight", "800").set("margin-bottom", "6px");
		agg.add(title);

		HorizontalLayout cats = new HorizontalLayout();
		cats.getStyle().set("gap", "12px");
		categoryCounts.forEach((cat, cnt) -> {
			HorizontalLayout item = new HorizontalLayout();
			item.setAlignItems(Alignment.CENTER);
			Icon ic = VaadinIcon.CUTLERY.create();
			ic.getStyle().set("font-size", "18px").set("color", "#ef4444");
			Span label = new Span(cat);
			label.getStyle().set("margin-left", "6px").set("font-weight", "700");
			Span badge = new Span(String.valueOf(cnt));
			badge.getElement().getThemeList().add("badge");
			badge.getStyle().set("background-color", "#fef3c7");
			badge.getStyle().set("color", "#92400e");
			badge.getStyle().set("padding", "2px 8px");
			badge.getStyle().set("border-radius", "12px");
			item.add(ic, label, badge);
			cats.add(item);
		});
		agg.add(cats);

		// top items as pill badges (show contributing orders on hover)
		List<Map.Entry<String,Integer>> topItems = itemCounts.entrySet().stream().sorted((a,b) -> b.getValue() - a.getValue()).limit(8).collect(Collectors.toList());
		HorizontalLayout itemsRow = new HorizontalLayout();
		itemsRow.getStyle().set("gap", "8px");
		for (var en : topItems) {
			Span pill = new Span(en.getKey() + " × " + en.getValue());
			pill.getStyle().set("background-color", "#eef2ff").set("color", "#1e293b");
			pill.getStyle().set("padding", "4px 10px");
			pill.getStyle().set("border-radius", "16px");
			// build tooltip from contributing order IDs (limit length)
			List<String> contrib = itemToOrders.getOrDefault(en.getKey(), List.of());
			if (!contrib.isEmpty()) {
				String joined = String.join(", ", contrib);
				if (joined.length() > 200) joined = joined.substring(0, 197) + "...";
				pill.getElement().setAttribute("title", "Orders: " + joined);
			}
			itemsRow.add(pill);
		}
		agg.add(itemsRow);
		add(agg);

		// also render individual order cards with quick actions
		for (OrderInfo o : orders) {
			VerticalLayout card = new VerticalLayout();
			card.getStyle().set("padding", "10px").set("border", "1px solid #e5e7eb");
			card.add(new Span("Order: " + o.id()));
			card.add(new Span("Items: " + (o.items() == null ? 0 : o.items().size())));
			card.add(new Span("Status: " + o.status()));
			Button markReady = new Button("Mark Ready", ev -> {
				orderFacade.updateStatus(o.id(), "READY");
				render();
			});
			card.add(markReady);
			add(card);
		}
	}
} 
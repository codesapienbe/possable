package com.possable.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.possable.service.ItemService;
import com.possable.service.OrderService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "service", layout = MainLayout.class)
@PageTitle("Service")
@PreAuthorize("hasAnyRole('SERVICE','MANAGEMENT')")
public class ServiceView extends VerticalLayout {

	private final ItemService itemService;
	private final OrderService orderService;

	public ServiceView(ItemService itemService, OrderService orderService) {
		this.itemService = itemService;
		this.orderService = orderService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Service"));

		Tabs tabs = new Tabs();
		Tab menuTab = new Tab("Menu");
		Tab ordersTab = new Tab("Orders");
		tabs.add(menuTab, ordersTab);

		Div content = new Div();
		content.setWidthFull();

		Map<Tab, Component> tabToContent = new HashMap<>();
		tabToContent.put(menuTab, new ItemListComponent(itemService));
		tabToContent.put(ordersTab, new OrdersComponent(orderService));

		tabs.addSelectedChangeListener(ev -> {
			content.removeAll();
			content.add(tabToContent.get(ev.getSelectedTab()));
		});

		add(tabs, content);
		// select first
		tabs.setSelectedTab(menuTab);
		content.add(tabToContent.get(menuTab));
	}

	@Deprecated
	private Component buildMenu() {
		HorizontalLayout tiles = new HorizontalLayout();
		tiles.setWidthFull();
		tiles.getStyle().set("flex-wrap", "wrap").set("gap", "12px");
		List<ItemService.Item> items = itemService.listItems(200);
		for (var it : items) {
			Button b = new Button(it.name() + "\n" + String.format("$%.2f", it.price()));
			b.addClassName("pos-tile");
			b.addClickListener(e -> {
				// minimal add-to-cart feedback for service
				Notification.show("Selected: " + it.name());
			});
			tiles.add(b);
		}
		return tiles;
	}

	// orders grid functionality moved to OrdersComponent
} 
package com.possable.view;

import java.util.List;

import com.possable.controller.OrderController.OrderDto;
import com.possable.service.OrderService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("POS Dashboard")
public class DashboardView extends VerticalLayout {

	private final OrderService orderService;

	public DashboardView(OrderService orderService) {
		this.orderService = orderService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		H1 header = new H1("Dashboard");
		add(header);

		HorizontalLayout stats = new HorizontalLayout();
		stats.setWidthFull();
		stats.getStyle().set("gap", "var(--lumo-space-m)");
		stats.add(createStat("Orders", Integer.toString(orderService.listOrders().size())), createStat("Revenue", "$0.00"), createStat("Printers", "0"));
		add(stats);

		add(new H3("Recent Orders"));
		Grid<OrderDto> recent = new Grid<>(OrderDto.class, false);
		recent.addColumn(OrderDto::getId).setHeader("ID").setAutoWidth(true);
		recent.addColumn(OrderDto::getStatus).setHeader("Status").setAutoWidth(true);
		recent.addColumn(o -> Integer.toString(o.getItems() == null ? 0 : o.getItems().size())).setHeader("Items");
		recent.setItems(getRecentOrders());
		add(recent);
	}

	private Component createStat(String label, String value) {
		VerticalLayout card = new VerticalLayout();
		card.getStyle().set("padding", "var(--lumo-space-m)").set("border", "1px solid var(--lumo-contrast-10pct)").set("border-radius", "8px").set("min-width", "160px");
		card.add(new H3(label), new Span(value));
		return card;
	}

	private List<OrderDto> getRecentOrders() {
		var all = orderService.listOrders();
		return all.size() <= 10 ? all : all.subList(0, 10);
	}
} 
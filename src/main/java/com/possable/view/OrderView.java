package com.possable.view;

import java.util.List;

import com.possable.service.OrderService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Orders")
public class OrderView extends VerticalLayout {

	public static record OrderDto(String id, String status, int itemsCount, String createdAt) {}

	private final Grid<OrderDto> grid = new Grid<>(OrderDto.class, false);

	public OrderView(OrderService orderService) {
		setPadding(true);
		setSpacing(true);
		setWidthFull();

		add(new H1("Orders"));

		grid.addColumn(OrderDto::id).setHeader("ID").setAutoWidth(true);
		grid.addColumn(OrderDto::status).setHeader("Status").setAutoWidth(true);
		grid.addColumn(order -> Integer.toString(order.itemsCount())).setHeader("Items").setAutoWidth(true);

		grid.setItems(mapOrders(orderService.listOrders()));
		add(grid);
	}

	private List<OrderDto> mapOrders(List<com.possable.controller.OrderController.OrderDto> orders) {
		return orders.stream()
			.map(o -> new OrderDto(o.getId(), o.getStatus(), o.getItems() == null ? 0 : o.getItems().size(), o.getCreatedAt() == null ? "" : o.getCreatedAt().toString()))
			.toList();
	}
} 
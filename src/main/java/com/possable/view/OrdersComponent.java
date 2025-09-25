package com.possable.view;

import java.util.List;

import com.possable.service.OrderService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class OrdersComponent extends VerticalLayout {

	public OrdersComponent(OrderService orderService) {
		setPadding(false);
		setSpacing(false);
		setWidthFull();
		Grid<com.possable.controller.OrderController.OrderDto> grid = new Grid<>(com.possable.controller.OrderController.OrderDto.class, false);
		grid.addColumn(com.possable.controller.OrderController.OrderDto::getId).setHeader("ID");
		grid.addColumn(o -> o.getStatus() == null ? "" : o.getStatus()).setHeader("Status");
		grid.addColumn(o -> Integer.toString(o.getItems() == null ? 0 : o.getItems().size())).setHeader("Items");
		grid.setItems(orderService.listOrders());
		add(grid);
	}
} 
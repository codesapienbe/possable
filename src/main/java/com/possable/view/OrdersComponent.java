package com.possable.view;

import java.util.List;

import com.possable.service.OrderService;
import com.possable.service.PrinterService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class OrdersComponent extends VerticalLayout {

	private final OrderService orderService;
	private final Grid<com.possable.controller.OrderController.OrderDto> grid = new Grid<>(com.possable.controller.OrderController.OrderDto.class, false);
	private final OrdersDetailComponent details;

	public OrdersComponent(OrderService orderService, PrinterService printerService, PrintJobService printJobService, PrintTemplateService templateService) {
		this.orderService = orderService;
		this.details = new OrdersDetailComponent(orderService, printerService, printJobService, templateService);
		setPadding(false);
		setSpacing(false);
		setWidthFull();
		grid.addColumn(com.possable.controller.OrderController.OrderDto::getId).setHeader("ID");
		grid.addColumn(o -> o.getStatus() == null ? "" : o.getStatus()).setHeader("Status");
		grid.addColumn(o -> Integer.toString(o.getItems() == null ? 0 : o.getItems().size())).setHeader("Items");
		grid.setItems(orderService.listOrders());
		grid.setSelectionMode(Grid.SelectionMode.SINGLE);
		grid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(d -> details.showOrder(d)));
		add(grid, details);
	}
} 
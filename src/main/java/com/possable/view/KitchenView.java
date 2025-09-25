package com.possable.view;

import com.possable.service.OrderService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.html.Div;

import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "kitchen", layout = MainLayout.class)
@PageTitle("Kitchen")
@PreAuthorize("hasAnyRole('KITCHEN','MANAGEMENT')")
public class KitchenView extends VerticalLayout {

	private final OrderService orderService;

	public KitchenView(OrderService orderService, com.possable.service.PrinterService printerService, com.possable.service.PrintJobService printJobService, com.possable.service.PrintTemplateService templateService) {
		this.orderService = orderService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Kitchen"));

		Tabs tabs = new Tabs();
		Tab ordersTab = new Tab("Orders");
		tabs.add(ordersTab);

		Div content = new Div();
		content.add(new OrdersComponent(orderService, printerService, printJobService, templateService));
		add(tabs, content);
	}

	private Component buildOrdersGrid() {
		Grid<com.possable.controller.OrderController.OrderDto> grid = new Grid<>(com.possable.controller.OrderController.OrderDto.class, false);
		grid.addColumn(com.possable.controller.OrderController.OrderDto::getId).setHeader("ID");
		grid.addColumn(o -> o.getStatus() == null ? "" : o.getStatus()).setHeader("Status");
		grid.addColumn(o -> Integer.toString(o.getItems() == null ? 0 : o.getItems().size())).setHeader("Items");
		grid.setItems(orderService.listOrders());
		return grid;
	}
} 
package com.possable.view;

import org.springframework.security.access.prepost.PreAuthorize;

import com.possable.service.ItemService;
import com.possable.service.OrderService;
import com.possable.service.PaymentService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.possable.service.PrinterService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "cashier", layout = MainLayout.class)
@PageTitle("Cashier")
@PreAuthorize("hasAnyRole('CASHIER','MANAGEMENT')")
public class CashierView extends HorizontalLayout {

	private final OrderService orderService;
	private final OrdersDetailComponent details;
	private final Grid<com.possable.controller.OrderController.OrderDto> grid = new Grid<>(com.possable.controller.OrderController.OrderDto.class, false);

	public CashierView(OrderService orderService, PrinterService printerService, PrintJobService printJobService, PrintTemplateService templateService, ItemService itemService, PaymentService paymentService) {
		this.orderService = orderService;
		this.details = new OrdersDetailComponent(orderService, printerService, printJobService, templateService, itemService, paymentService);

		setPadding(true);
		setSpacing(true);
		setWidthFull();
		setHeightFull();
		addClassName("pos-root");
		addClassName("pos-split");

		// header
		VerticalLayout leftColumn = new VerticalLayout();
		leftColumn.setPadding(false);
		leftColumn.setSpacing(false);
		leftColumn.setWidthFull();
		leftColumn.setHeightFull();
		leftColumn.addClassName("left");
		leftColumn.add(new H1("Cashier - Orders"));

		// configure grid (compact for cashier workflow)
		grid.addColumn(com.possable.controller.OrderController.OrderDto::getId).setHeader("ID");
		grid.addColumn(o -> o.getStatus() == null ? "" : o.getStatus()).setHeader("Status");
		grid.addColumn(o -> Integer.toString(o.getItems() == null ? 0 : o.getItems().size())).setHeader("Items");
		grid.setItems(orderService.listOrders());
		grid.setWidthFull();
		grid.setHeightFull();
		grid.setSelectionMode(Grid.SelectionMode.SINGLE);
		grid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(d -> details.showOrder(d)));

		leftColumn.add(grid);
		leftColumn.expand(grid);

		// layout split: orders (left) and details/payment (right)
		this.setWidthFull();
		this.setHeightFull();
		// left takes priority
		leftColumn.setWidth("60%");
		details.setWidth("40%");
		details.setHeightFull();
		details.addClassName("right");

		add(leftColumn, details);
		// expand the left column so the grid gets available space
		expand(leftColumn);
	}
} 
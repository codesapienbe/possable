package com.possable.view;

import com.possable.service.ItemService;
import com.possable.service.OrderService;
import com.possable.service.PaymentService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.possable.service.PrinterService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class OrdersComponent extends VerticalLayout {

	private final OrderService orderService;
	private final Grid<com.possable.controller.OrderController.OrderDto> grid = new Grid<>(com.possable.controller.OrderController.OrderDto.class, false);
	private final OrdersDetailComponent details;
	private final ItemService itemService;
	private final PaymentService paymentService;

	public OrdersComponent(OrderService orderService, PrinterService printerService, PrintJobService printJobService, PrintTemplateService templateService, ItemService itemService, PaymentService paymentService) {
		this.orderService = orderService;
		this.itemService = itemService;
		this.paymentService = paymentService;
		this.details = new OrdersDetailComponent(orderService, printerService, printJobService, templateService, itemService, paymentService);
		setPadding(false);
		setSpacing(false);
		setWidthFull();
		setHeightFull();
		addClassName("pos-split");
		grid.addClassName("left");
		details.addClassName("right");
		grid.addColumn(com.possable.controller.OrderController.OrderDto::getId).setHeader("ID");
		grid.addColumn(o -> o.getStatus() == null ? "" : o.getStatus()).setHeader("Status");
		grid.addColumn(o -> Integer.toString(o.getItems() == null ? 0 : o.getItems().size())).setHeader("Items");
		grid.setItems(orderService.listOrders());
		// ensure the grid stretches to full available width and takes priority in layout
		grid.setWidthFull();
		grid.setHeightFull();
		grid.setSelectionMode(Grid.SelectionMode.SINGLE);
		grid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(d -> details.showOrder(d)));

		// inline quick actions for faster workflow (open / request payment / mark ready / void)
		grid.addComponentColumn(o -> {
			HorizontalLayout actions = new HorizontalLayout();
			actions.setSpacing(true);
			Button open = new Button("Open", evt -> details.showOrder(o));
			Button reqPay = new Button("Req Pay", evt -> {
				if (paymentService != null) {
					double amount = 0.0; // approximate using item service
					if (o.getItems() != null) {
						for (String id : o.getItems()) { var it = itemService.findById(id); if (it != null) amount += it.price(); }
					}
					paymentService.createPayment(o.getId(), amount, "card");
					Notification.show("Payment requested for $" + String.format("%.2f", amount));
				}
			});
			Button ready = new Button("Ready", evt -> {
				String prev = o.getStatus();
				orderService.updateStatus(o.getId(), "READY");
				Notification n = new Notification();
				n.setDuration(8000);
				Button undo = new Button("Undo", u -> { orderService.updateStatus(o.getId(), prev); n.close(); });
				n.add(new HorizontalLayout(new Span("Marked READY"), undo));
				n.open();
			});
			Button voidBtn = new Button("Void", evt -> {
				String prev = o.getStatus();
				orderService.updateStatus(o.getId(), "VOID");
				Notification n = new Notification();
				n.setDuration(8000);
				Button undo = new Button("Undo", u -> { orderService.updateStatus(o.getId(), prev); n.close(); });
				n.add(new HorizontalLayout(new Span("Order voided"), undo));
				n.open();
			});
			actions.add(open, reqPay, ready, voidBtn);
			return actions;
		}).setHeader("Actions").setAutoWidth(true);
		add(grid, details);
		// expand the grid so it fills available vertical/horizontal space within this component
		setFlexGrow(2, grid);
		setFlexGrow(1, details);
		expand(grid);
	}
} 
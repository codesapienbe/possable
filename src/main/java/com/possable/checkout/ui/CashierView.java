package com.possable.checkout.ui;

import org.springframework.security.access.prepost.PreAuthorize;

import com.possable.order.OrderFacade;
import com.possable.inventory.InventoryFacade;
import com.possable.print.PrintFacade;
import com.possable.checkout.CheckoutFacade;
import com.possable.order.ui.OrdersDetailComponent;
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

	private final OrderFacade orderService;
	private final OrdersDetailComponent details;
	private final Grid<com.possable.order.OrderFacade.OrderInfo> grid = new Grid<>(com.possable.order.OrderFacade.OrderInfo.class, false);

	public CashierView(OrderFacade orderService, PrintFacade printFacade, InventoryFacade inventoryFacade, CheckoutFacade checkoutFacade) {
		this.orderService = orderService;
		this.details = new OrdersDetailComponent(orderService, printFacade, inventoryFacade, checkoutFacade);

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
		grid.addColumn(com.possable.order.OrderFacade.OrderInfo::getId).setHeader("ID");
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

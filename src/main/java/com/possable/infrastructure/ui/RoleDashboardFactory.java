package com.possable.infrastructure.ui;

import org.springframework.stereotype.Component;

import com.possable.order.OrderFacade;
import com.possable.inventory.InventoryFacade;
import com.possable.print.PrintFacade;
import com.possable.checkout.CheckoutFacade;

// Import views from their modules
import com.possable.inventory.ui.ItemListComponent;
import com.possable.order.ui.OrdersComponent;
import com.possable.print.ui.PrintersComponent;
import com.possable.print.ui.PrintJobsComponent;

@Component
public class RoleDashboardFactory {

	private final InventoryFacade inventoryFacade;
	private final OrderFacade orderFacade;
	private final PrintFacade printFacade;
	private final CheckoutFacade checkoutFacade;

	public RoleDashboardFactory(InventoryFacade inventoryFacade, OrderFacade orderFacade, PrintFacade printFacade, CheckoutFacade checkoutFacade) {
		this.inventoryFacade = inventoryFacade;
		this.orderFacade = orderFacade;
		this.printFacade = printFacade;
		this.checkoutFacade = checkoutFacade;
	}

	public ItemListComponent createItemListComponent() {
		return new ItemListComponent(inventoryFacade, orderFacade);
	}

	public OrdersComponent createOrdersComponent() {
		return new OrdersComponent(orderFacade, printFacade, inventoryFacade, checkoutFacade);
	}

	public PrintersComponent createPrintersComponent() {
		return new PrintersComponent(printFacade);
	}

	public PrintJobsComponent createPrintJobsComponent() {
		return new PrintJobsComponent(printFacade);
	}
} 

package com.possable.view;

import org.springframework.stereotype.Component;

import com.possable.service.ItemService;
import com.possable.service.OrderService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.possable.service.PrinterService;
import com.possable.service.PaymentService;

@Component
public class RoleDashboardFactory {

	private final ItemService itemService;
	private final OrderService orderService;
	private final PrinterService printerService;
	private final PrintJobService printJobService;
	private final PrintTemplateService templateService;
	private final PaymentService paymentService;

	public RoleDashboardFactory(ItemService itemService, OrderService orderService, PrinterService printerService, PrintJobService printJobService, PrintTemplateService templateService, PaymentService paymentService) {
		this.itemService = itemService;
		this.orderService = orderService;
		this.printerService = printerService;
		this.printJobService = printJobService;
		this.templateService = templateService;
		this.paymentService = paymentService;
	}

	public ItemListComponent createItemListComponent() {
		return new ItemListComponent(itemService, orderService);
	}

	public OrdersComponent createOrdersComponent() {
		return new OrdersComponent(orderService, printerService, printJobService, templateService, itemService, paymentService);
	}

	public PrintersComponent createPrintersComponent() {
		return new PrintersComponent(printerService);
	}

	public PrintJobsComponent createPrintJobsComponent() {
		return new PrintJobsComponent(printJobService);
	}
} 
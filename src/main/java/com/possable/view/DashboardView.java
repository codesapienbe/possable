package com.possable.view;

import java.util.List;

import com.possable.controller.OrderController.OrderDto;
import com.possable.service.ItemService;
import com.possable.service.OrderService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.possable.service.PrinterService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("POS Dashboard")
public class DashboardView extends VerticalLayout {

	private final OrderService orderService;
	private final PrintJobService printJobService;

	public DashboardView(OrderService orderService, PrinterService printerService, ItemService itemService, PrintTemplateService templateService, PrintJobService printJobService) {
		this.orderService = orderService;
		this.printJobService = printJobService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		H1 header = new H1("POS");
		header.getStyle().set("font-size", "1.8em");
		add(header);

		HorizontalLayout stats = new HorizontalLayout();
		stats.setWidthFull();
		stats.getStyle().set("gap", "var(--lumo-space-m)");
		int ordersCount = orderService.listOrders().size();
		double revenue = computeRevenue(orderService.listOrders(), itemService);
		int printersCount = printerService.listPrinters().size();
		stats.add(createStat("Orders", Integer.toString(ordersCount)), createStat("Revenue", String.format("$%.2f", revenue)), createStat("Printers", Integer.toString(printersCount)));
		add(stats);

		// Manage printers/templates and run full demo
		Button createPrinters = new Button("Setup Demo Printers", evt -> {
			if (!printerService.listPrinters().isEmpty() || !templateService.listTemplates().isEmpty()) {
				Notification.show("Printers/templates already exist", 3000, Notification.Position.TOP_END);
				return;
			}
			printerService.registerPrinter("Kitchen Printer", "kitchen", "Main kitchen order printer");
			printerService.registerPrinter("Bar Printer", "bar", "Bar drinks printer");
			printerService.registerPrinter("Receipt Printer", "receipt", "Customer receipts");

			templateService.createTemplate("kitchen", "kitchen-default", "KITCHEN\nOrder: {{orderId}}\nItems: {{items}}\nNotes: {{notes}}");
			templateService.createTemplate("bar", "bar-default", "BAR\nOrder: {{orderId}}\nItems: {{items}}");
			templateService.createTemplate("receipt", "receipt-default", "RECEIPT\nOrder: {{orderId}}\nTotal: {{total}}\nThank you!");

			Notification.show("Demo printers and templates created", 3000, Notification.Position.TOP_END);
			UI.getCurrent().getPage().reload();
		});

		Button runDemo = new Button("Create Sample Order", evt -> {
			// ensure printers/templates exist
			if (printerService.listPrinters().isEmpty() || templateService.listTemplates().isEmpty()) {
				printerService.registerPrinter("Kitchen Printer", "kitchen", "Main kitchen order printer");
				printerService.registerPrinter("Bar Printer", "bar", "Bar drinks printer");
				printerService.registerPrinter("Receipt Printer", "receipt", "Customer receipts");
				templateService.createTemplate("kitchen", "kitchen-default", "KITCHEN\nOrder: {{orderId}}\nItems: {{items}}\nNotes: {{notes}}");
				templateService.createTemplate("bar", "bar-default", "BAR\nOrder: {{orderId}}\nItems: {{items}}");
				templateService.createTemplate("receipt", "receipt-default", "RECEIPT\nOrder: {{orderId}}\nTotal: {{total}}\nThank you!");
			}

			// ensure demo items exist
			if (itemService.listItems(200).isEmpty()) {
				itemService.createItem("Espresso", "Strong black coffee", 2.5, true);
				itemService.createItem("Latte", "Milky coffee", 3.5, true);
				itemService.createItem("Burger", "Beef burger with lettuce", 8.0, true);
				itemService.createItem("Fries", "Crispy fries", 3.0, true);
			}

			// create a sample order using first two items
			var menu = itemService.listItems(200).stream().map(i -> i.id()).toList();
			if (menu.isEmpty()) {
				Notification.show("No items available for demo", 3000, Notification.Position.TOP_END);
				return;
			}
			var orderItems = menu.size() >= 2 ? List.of(menu.get(0), menu.get(1)) : List.of(menu.get(0));
			var sampleOrder = orderService.createOrder(orderItems, "Demo order");

			// create print jobs
			var templates = templateService.listTemplates();
			int created = 0;
			for (var p : printerService.listPrinters()) {
				var tpl = templates.stream().filter(t -> t.printerCategory().equals(p.category())).findFirst();
				if (tpl.isPresent()) {
					printJobService.createJob(sampleOrder.getId(), p.id(), tpl.get().id());
					created++;
				}
			}

			if (created > 0) {
				orderService.updateStatus(sampleOrder.getId(), "IN_PREPARATION");
			}

			Notification.show("Demo order created: " + sampleOrder.getId() + " (print jobs created: " + created + ")", 3000, Notification.Position.TOP_END);
			UI.getCurrent().navigate("print-jobs-ui");
		});

		HorizontalLayout demoActions = new HorizontalLayout(createPrinters, runDemo);
		add(demoActions);

		add(new H3("Recent Orders"));
		Grid<OrderDto> recent = new Grid<>(OrderDto.class, false);
		recent.addColumn(OrderDto::getId).setHeader("ID").setAutoWidth(true);
		recent.addColumn(OrderDto::getStatus).setHeader("Status").setAutoWidth(true);
		recent.addColumn(o -> Integer.toString(o.getItems() == null ? 0 : o.getItems().size())).setHeader("Items");
		recent.setItems(getRecentOrders());
		add(recent);
	}

	private double computeRevenue(List<OrderDto> orders, ItemService itemService) {
		double total = 0.0;
		for (OrderDto o : orders) {
			if (o.getItems() == null) continue;
			for (String itemId : o.getItems()) {
				var it = itemService.findById(itemId);
				if (it != null) total += it.price();
			}
		}
		return total;
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
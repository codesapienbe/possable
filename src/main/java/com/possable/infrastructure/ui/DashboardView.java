package com.possable.infrastructure.ui;

import java.util.List;
import java.util.Map;

import com.possable.order.OrderFacade;
import com.possable.inventory.InventoryFacade;
import com.possable.print.PrintFacade;
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

@Route(value = "dashboard", layout = com.possable.user.UserMainLayout.class)
@PageTitle("POS Dashboard")
public class DashboardView extends VerticalLayout {

	private final OrderFacade orderFacade;
	private final PrintFacade printFacade;
	private final InventoryFacade inventoryFacade;

	public DashboardView(OrderFacade orderFacade, PrintFacade printFacade, InventoryFacade inventoryFacade) {
		this.orderFacade = orderFacade;
		this.printFacade = printFacade;
		this.inventoryFacade = inventoryFacade;
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
		
		@SuppressWarnings("unchecked")
		List<OrderFacade.OrderInfo> orders = (List<OrderFacade.OrderInfo>) orderFacade.listOrdersPaged(Map.of()).get("items");
		int ordersCount = orders.size();
		double revenue = computeRevenue(orders, inventoryFacade);
		int printersCount = printFacade.listPrinters(Map.of()).size();
		stats.add(createStat("Orders", Integer.toString(ordersCount)), createStat("Revenue", String.format("$%.2f", revenue)), createStat("Printers", Integer.toString(printersCount)));
		add(stats);

		// Manage printers/templates and run full demo
		Button createPrinters = new Button("Setup Demo Printers", evt -> {
			if (!printFacade.listPrinters(Map.of()).isEmpty() || !printFacade.listTemplates(Map.of()).isEmpty()) {
				Notification.show("Printers/templates already exist", 3000, Notification.Position.TOP_END);
				return;
			}
			printFacade.registerPrinter("Kitchen Printer", "kitchen", "Main kitchen order printer");
			printFacade.registerPrinter("Bar Printer", "bar", "Bar drinks printer");
			printFacade.registerPrinter("Receipt Printer", "receipt", "Customer receipts");

			printFacade.createTemplate("kitchen", "kitchen-default", "KITCHEN\nOrder: {{orderId}}\nItems: {{items}}\nNotes: {{notes}}");
			printFacade.createTemplate("bar", "bar-default", "BAR\nOrder: {{orderId}}\nItems: {{items}}");
			printFacade.createTemplate("receipt", "receipt-default", "RECEIPT\nOrder: {{orderId}}\nTotal: {{total}}\nThank you!");

			Notification.show("Demo printers and templates created", 3000, Notification.Position.TOP_END);
			UI.getCurrent().getPage().reload();
		});

		Button runDemo = new Button("Create Sample Order", evt -> {
			// ensure printers/templates exist
			if (printFacade.listPrinters(Map.of()).isEmpty() || printFacade.listTemplates(Map.of()).isEmpty()) {
				printFacade.registerPrinter("Kitchen Printer", "kitchen", "Main kitchen order printer");
				printFacade.registerPrinter("Bar Printer", "bar", "Bar drinks printer");
				printFacade.registerPrinter("Receipt Printer", "receipt", "Customer receipts");
				printFacade.createTemplate("kitchen", "kitchen-default", "KITCHEN\nOrder: {{orderId}}\nItems: {{items}}\nNotes: {{notes}}");
				printFacade.createTemplate("bar", "bar-default", "BAR\nOrder: {{orderId}}\nItems: {{items}}");
				printFacade.createTemplate("receipt", "receipt-default", "RECEIPT\nOrder: {{orderId}}\nTotal: {{total}}\nThank you!");
			}

			// ensure demo items exist
			@SuppressWarnings("unchecked")
			List<InventoryFacade.ItemInfo> items = (List<InventoryFacade.ItemInfo>) inventoryFacade.listItemsPaged(Map.of("limit", "200")).get("items");
			if (items.isEmpty()) {
				inventoryFacade.createItem("Espresso", "Strong black coffee", 2.5, true);
				inventoryFacade.createItem("Latte", "Milky coffee", 3.5, true);
				inventoryFacade.createItem("Burger", "Beef burger with lettuce", 8.0, true);
				inventoryFacade.createItem("Fries", "Crispy fries", 3.0, true);
			}

			// create a sample order using first two items
			@SuppressWarnings("unchecked")
			List<InventoryFacade.ItemInfo> menu = (List<InventoryFacade.ItemInfo>) inventoryFacade.listItemsPaged(Map.of("limit", "200")).get("items");
			var menuIds = menu.stream().map(i -> i.id()).toList();
			if (menuIds.isEmpty()) {
				Notification.show("No items available for demo", 3000, Notification.Position.TOP_END);
				return;
			}
			var orderItems = menuIds.size() >= 2 ? List.of(menuIds.get(0), menuIds.get(1)) : List.of(menuIds.get(0));
			var sampleOrder = orderFacade.createOrder(orderItems, "Demo order");

			// create print jobs
			var templates = printFacade.listTemplates(Map.of());
			int created = 0;
			for (var p : printFacade.listPrinters(Map.of())) {
				var tpl = templates.stream().filter(t -> t.printerCategory().equals(p.category())).findFirst();
				if (tpl.isPresent()) {
					printFacade.createJob(sampleOrder.id(), p.id(), tpl.get().id());
					created++;
				}
			}

			if (created > 0) {
				orderFacade.updateStatus(sampleOrder.id(), "IN_PREPARATION");
			}

			Notification.show("Demo order created: " + sampleOrder.id() + " (print jobs created: " + created + ")", 3000, Notification.Position.TOP_END);
			UI.getCurrent().navigate("print-jobs-ui");
		});

		HorizontalLayout demoActions = new HorizontalLayout(createPrinters, runDemo);
		add(demoActions);

		add(new H3("Recent Orders"));
		Grid<OrderFacade.OrderInfo> recent = new Grid<>(OrderFacade.OrderInfo.class, false);
		recent.addColumn(OrderFacade.OrderInfo::id).setHeader("ID").setAutoWidth(true);
		recent.addColumn(OrderFacade.OrderInfo::status).setHeader("Status").setAutoWidth(true);
		recent.addColumn(o -> Integer.toString(o.items() == null ? 0 : o.items().size())).setHeader("Items");
		recent.setItems(getRecentOrders());
		add(recent);
	}

	private double computeRevenue(List<OrderFacade.OrderInfo> orders, InventoryFacade inventoryFacade) {
		double total = 0.0;
		for (OrderFacade.OrderInfo o : orders) {
			if (o.items() == null) continue;
			for (String itemId : o.items()) {
				var it = inventoryFacade.findById(itemId);
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

	@SuppressWarnings("unchecked")
	private List<OrderFacade.OrderInfo> getRecentOrders() {
		var all = (List<OrderFacade.OrderInfo>) orderFacade.listOrdersPaged(Map.of()).get("items");
		return all.size() <= 10 ? all : all.subList(0, 10);
	}

} 



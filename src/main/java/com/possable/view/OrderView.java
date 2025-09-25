package com.possable.view;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.possable.service.OrderService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.possable.service.PrinterService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Orders")
public class OrderView extends VerticalLayout {

	public static record OrderDto(String id, String status, int itemsCount, String createdAt) {}

	private final Grid<OrderDto> grid = new Grid<>(OrderDto.class, false);
	private final VerticalLayout details = new VerticalLayout();
	private final OrderService orderService;
	private final PrinterService printerService;
	private final PrintJobService printJobService;
	private final PrintTemplateService templateService;
	private Registration pollRegistration;
	private final ComboBox<String> statusFilter = new ComboBox<>("Status");

	public OrderView(OrderService orderService, PrinterService printerService, PrintJobService printJobService, PrintTemplateService templateService) {
		this.orderService = orderService;
		this.printerService = printerService;
		this.printJobService = printJobService;
		this.templateService = templateService;

		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Orders"));

		// Controls: status filter and manual refresh
		statusFilter.setItems("ALL", "PENDING", "IN_PREPARATION", "READY");
		statusFilter.setValue("ALL");
		statusFilter.addValueChangeListener(e -> applyFilter(e.getValue()));
		statusFilter.addClassName("pos-filter");
		Button refreshButton = new Button("Refresh", evt -> refresh());
		HorizontalLayout controls = new HorizontalLayout(statusFilter, refreshButton);
		controls.setWidthFull();
		add(controls);

		HorizontalLayout main = new HorizontalLayout();
		main.setWidthFull();
		main.setSpacing(true);

		grid.addColumn(OrderDto::id).setHeader("ID").setAutoWidth(true);
		// status as colored badge for quick scanning
		grid.addComponentColumn(o -> {
			Span badge = new Span(o.status());
			String s = o.status() == null ? "" : o.status().toUpperCase();
			switch (s) {
			case "PENDING" -> badge.getElement().getThemeList().add("badge");
			case "IN_PREPARATION" -> badge.getElement().getThemeList().add("badge contrast");
			case "READY" -> badge.getElement().getThemeList().add("badge success");
			default -> badge.getElement().getThemeList().add("badge");
			}
			return badge;
		}).setHeader("Status").setAutoWidth(true);

		grid.addColumn(order -> Integer.toString(order.itemsCount())).setHeader("Items").setAutoWidth(true);
		grid.addClassName("pos-grid");
		grid.setItems(mapOrders(orderService.listOrders()));
		grid.setSelectionMode(Grid.SelectionMode.SINGLE);
		grid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(this::showDetails));

		main.add(grid, details);
		main.setFlexGrow(2, grid);
		main.setFlexGrow(1, details);
		details.addClassName("pos-details");

		addAttachListener(evt -> {
			UI ui = evt.getUI();
			ui.setPollInterval(2000); // poll every 2s while attached
			pollRegistration = ui.addPollListener(pe -> refresh());
		});

		addDetachListener(evt -> {
			if (pollRegistration != null) {
				pollRegistration.remove();
				pollRegistration = null;
			}
			// stop polling when detached
			evt.getUI().setPollInterval(0);
		});

		add(main);
	}

	private void applyFilter(String status) {
		List<OrderDto> all = mapOrders(orderService.listOrders());
		if (status == null || "ALL".equals(status)) {
			grid.setItems(all);
			return;
		}
		grid.setItems(all.stream().filter(o -> status.equals(o.status())).collect(Collectors.toList()));
	}

	private void showDetails(OrderDto order) {
		details.removeAll();
		details.add(new H1("Order " + order.id()));
		details.add(new Pre("Status: " + order.status()));
		details.add(new Pre("Items: " + order.itemsCount()));

		CheckboxGroup<PrinterService.Printer> printers = new CheckboxGroup<>();
		printers.setLabel("Send to printers");
		var available = printerService.listPrinters();
		printers.setItems(available);
		printers.setItemLabelGenerator(p -> p.name() + " (" + p.category() + ")");

		Button send = new Button("Send to Kitchen", evt -> {
			if (order == null) return;
			Set<PrinterService.Printer> selected = printers.getSelectedItems();
			if (selected == null || selected.isEmpty()) {
				Notification.show("Select at least one printer");
				return;
			}

			var templates = templateService.listTemplates();
			int created = 0;
			var missing = new StringBuilder();
			for (PrinterService.Printer p : selected) {
				var tpl = templates.stream().filter(t -> t.printerCategory().equals(p.category())).findFirst();
				if (tpl.isPresent()) {
					printJobService.createJob(order.id(), p.id(), tpl.get().id());
					created++;
				} else {
					if (missing.length() > 0) missing.append(", ");
					missing.append(p.name());
				}
			}

			orderService.updateStatus(order.id(), "IN_PREPARATION");
			String msg = "Sent to printers (print jobs created: " + created + ")";
			if (missing.length() > 0) {
				msg += "; no template for: " + missing.toString();
			}
			Notification.show(msg);
		});

		Button ready = new Button("Mark Ready", evt -> {
			if (order == null) return;
			orderService.updateStatus(order.id(), "READY");
			Notification.show("Order marked READY");
		});

		Button refresh = new Button("Refresh", evt -> refresh());

		details.add(printers, new HorizontalLayout(send, ready, refresh));
	}

	private void refresh() {
		var orders = orderService.listOrders();
		// update grid on UI thread
		getUI().ifPresent(ui -> ui.access(() -> {
			grid.setItems(mapOrders(orders));
			// re-apply filter so UI keeps selection
			if (statusFilter.getValue() != null && !"ALL".equals(statusFilter.getValue())) {
				applyFilter(statusFilter.getValue());
			}
		}));
		details.removeAll();
	}

	private List<OrderDto> mapOrders(List<com.possable.controller.OrderController.OrderDto> orders) {
		return orders.stream()
			.map(o -> new OrderDto(o.getId(), o.getStatus(), o.getItems() == null ? 0 : o.getItems().size(), o.getCreatedAt() == null ? "" : o.getCreatedAt().toString()))
			.toList();
	}
} 
package com.possable.view;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.possable.service.ItemService;
import com.possable.service.OrderService;
import com.possable.service.PaymentService;
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
	private final ItemService itemService;
	private final PaymentService paymentService;
	private Registration pollRegistration;
	private final ComboBox<String> statusFilter = new ComboBox<>("Status");
	// debounce & skeleton sizing helpers
	private volatile long lastRefreshRequestMs = 0L;
	private int lastKnownSkeletonCount = 6;
	private final int pollDebounceMs = 1500; // ms

	public OrderView(OrderService orderService, PrinterService printerService, PrintJobService printJobService, PrintTemplateService templateService, ItemService itemService, PaymentService paymentService) {
		this.orderService = orderService;
		this.printerService = printerService;
		this.printJobService = printJobService;
		this.templateService = templateService;
		this.itemService = itemService;
		this.paymentService = paymentService;

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
		main.setHeightFull();
		main.setSpacing(true);
		main.addClassName("pos-split");

		grid.addClassName("left");
		details.addClassName("right");
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

		// inline quick actions column
		grid.addComponentColumn(o -> {
			HorizontalLayout actions = new HorizontalLayout();
			Button open = new Button("Open", evt -> showDetails(o));
			Button reqPay = new Button("Req Pay", evt -> {
				double amount = 0.0;
				if (o.itemsCount() > 0) {
					// best-effort approximate: ask orderService for details or sum using itemService if available
					var full = orderService.findById(o.id());
					if (full != null && full.getItems() != null) {
						for (String id : full.getItems()) { var it = itemService.findById(id); if (it != null) amount += it.price(); }
					}
				}
				paymentService.createPayment(o.id(), amount, "card");
				Notification.show("Payment requested for $" + String.format("%.2f", amount));
			});
			Button ready = new Button("Ready", evt -> {
				String prev = o.status();
				orderService.updateStatus(o.id(), "READY");
				Notification n = new Notification();
				n.setDuration(8000);
				Button undo = new Button("Undo", u -> { orderService.updateStatus(o.id(), prev); n.close(); });
				n.add(new HorizontalLayout(new Span("Marked READY"), undo));
				n.open();
			});
			Button voidBtn = new Button("Void", evt -> {
				String prev = o.status();
				orderService.updateStatus(o.id(), "VOID");
				Notification n = new Notification();
				n.setDuration(8000);
				Button undo = new Button("Undo", u -> { orderService.updateStatus(o.id(), prev); n.close(); });
				n.add(new HorizontalLayout(new Span("Order voided"), undo));
				n.open();
			});
			actions.add(open, reqPay, ready, voidBtn);
			return actions;
		}).setHeader("Actions");

		main.add(grid, details);
		main.setFlexGrow(2, grid);
		main.setFlexGrow(1, details);
		grid.setHeightFull();
		details.setHeightFull();
		details.addClassName("pos-details");

		addAttachListener(evt -> {
			UI ui = evt.getUI();
			ui.setPollInterval(2000); // poll every 2s while attached
			pollRegistration = ui.addPollListener(pe -> {
				long now = System.currentTimeMillis();
				if (now - lastRefreshRequestMs > pollDebounceMs) {
					lastRefreshRequestMs = now;
					refresh();
				}
			});
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
		// show skeletons immediately while we fetch updated orders in background
		int skeletonCount = computeSkeletonCount();
		getUI().ifPresent(ui -> ui.access(() -> showSkeletons(skeletonCount)));
		CompletableFuture.runAsync(() -> {
			var orders = orderService.listOrders();
			getUI().ifPresent(ui -> ui.access(() -> {
				grid.setItems(mapOrders(orders));
				// re-apply filter so UI keeps selection
				if (statusFilter.getValue() != null && !"ALL".equals(statusFilter.getValue())) {
					applyFilter(statusFilter.getValue());
				}
				// clear details pane after update to avoid stale state
				details.removeAll();
				// update lastKnownSkeletonCount after successful refresh
				lastKnownSkeletonCount = skeletonCount;
			}));
		}).exceptionally(ex -> {
			getUI().ifPresent(ui -> ui.access(() -> {
				Notification.show("Failed to refresh orders");
				// optionally clear skeletons or keep last known items
			}));
			return null;
		});
	}

	private void showSkeletons(int count) {
		List<OrderDto> placeholders = java.util.stream.IntStream.range(0, Math.max(1, count)).mapToObj(i -> new OrderDto("loading-" + i, "", 0, "")).toList();
		grid.setItems(placeholders);
	}

	private double parseTotal(String text) {
		try { return Double.parseDouble(text.replaceAll("[^0-9\\.]+", "")); } catch (Exception e) { return 0.0; }
	}

	private List<OrderDto> mapOrders(List<com.possable.controller.OrderController.OrderDto> orders) {
		return orders.stream()
			.map(o -> new OrderDto(o.getId(), o.getStatus(), o.getItems() == null ? 0 : o.getItems().size(), o.getCreatedAt() == null ? "" : o.getCreatedAt().toString()))
			.toList();
	}

	private int computeSkeletonCount() {
		// Prefer last known value (updated after successful refresh). Fall back to 6.
		return lastKnownSkeletonCount > 0 ? lastKnownSkeletonCount : 6;
	}
} 
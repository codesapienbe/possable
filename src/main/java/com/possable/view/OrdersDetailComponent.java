package com.possable.view;

import java.util.List;
import java.util.Set;

import com.possable.service.OrderService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.possable.service.PrinterService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class OrdersDetailComponent extends VerticalLayout {

	private final OrderService orderService;
	private final PrinterService printerService;
	private final PrintJobService printJobService;
	private final PrintTemplateService templateService;
	private CheckboxGroup<PrinterService.Printer> printers;
	private Button send;
	private Button ready;

	public OrdersDetailComponent(OrderService orderService, PrinterService printerService, PrintJobService printJobService, PrintTemplateService templateService) {
		this.orderService = orderService;
		this.printerService = printerService;
		this.printJobService = printJobService;
		this.templateService = templateService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-details");
	}

	public void showOrder(com.possable.controller.OrderController.OrderDto order) {
		removeAll();
		if (order == null) return;
		add(new H1("Order " + order.getId()));
		add(new Pre("Status: " + order.getStatus()));
		add(new Pre("Items: " + (order.getItems() == null ? 0 : order.getItems().size())));

		// create UI controls once and reuse
		this.printers = new CheckboxGroup<>();
		printers.setLabel("Send to printers");
		if (printerService != null) {
			var available = printerService.listPrinters();
			printers.setItems(available);
			printers.setItemLabelGenerator(p -> p.name() + " (" + p.category() + ")");
		}

		this.send = new Button("Send to Kitchen", evt -> {
			if (order == null) return;
			Set<PrinterService.Printer> selected = printers.getSelectedItems();
			if (selected == null || selected.isEmpty()) {
				Notification.show("Select at least one printer");
				return;
			}

			var templates = templateService == null ? List.<com.possable.service.PrintTemplateService.Template>of() : templateService.listTemplates();
			int created = 0;
			var missing = new StringBuilder();
			for (PrinterService.Printer p : selected) {
				var tpl = templates.stream().filter(t -> t.printerCategory().equals(p.category())).findFirst();
				if (tpl.isPresent() && printJobService != null) {
					printJobService.createJob(order.getId(), p.id(), tpl.get().id());
					created++;
				} else {
					if (missing.length() > 0) missing.append(", ");
					missing.append(p.name());
				}
			}

			orderService.updateStatus(order.getId(), "IN_PREPARATION");
			String msg = "Sent to printers (print jobs created: " + created + ")";
			if (missing.length() > 0) msg += "; no template for: " + missing.toString();
			Notification.show(msg);
		});

		this.ready = new Button("Mark Ready", evt -> {
			if (order == null) return;
			orderService.updateStatus(order.getId(), "READY");
			Notification.show("Order marked READY");
		});

		Button refresh = new Button("Refresh", evt -> {
			// no-op here; parent component may refresh list
		});

		// server-side permission checks
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean hasKitchenRole = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_KITCHEN") || a.getAuthority().equals("ROLE_MANAGEMENT"));
		boolean hasSendRole = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE") || a.getAuthority().equals("ROLE_KITCHEN") || a.getAuthority().equals("ROLE_MANAGEMENT"));

		// disable/hide printer controls if services missing or user unauthorized
		boolean printerAvailable = printerService != null && printJobService != null && templateService != null;
		if (!printerAvailable || !hasSendRole) {
			// hide printers and send button
			printers.setVisible(false);
			send.setVisible(false);
		}
		if (!hasKitchenRole) {
			ready.setVisible(false);
		}

		add(printers, new HorizontalLayout(send, ready, refresh));
	}
} 
package com.possable.view;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.possable.service.ItemService;
import com.possable.service.OrderService;
import com.possable.service.PaymentService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.possable.service.PrinterService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
// using Span for inline labels

public class OrdersDetailComponent extends VerticalLayout {

	private final OrderService orderService;
	private final PrinterService printerService;
	private final PrintJobService printJobService;
	private final PrintTemplateService templateService;
	private CheckboxGroup<PrinterService.Printer> printers;
	private Button send;
	private Button ready;
	private final ItemService itemService;
	private final PaymentService paymentService;

	public OrdersDetailComponent(OrderService orderService, PrinterService printerService, PrintJobService printJobService, PrintTemplateService templateService, ItemService itemService, PaymentService paymentService) {
		this.orderService = orderService;
		this.printerService = printerService;
		this.printJobService = printJobService;
		this.templateService = templateService;
		this.itemService = itemService;
		this.paymentService = paymentService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-details");
	}

	@SuppressWarnings("unused")
	public void showOrder(com.possable.controller.OrderController.OrderDto order) {
		removeAll();
		if (order == null) return;
		add(new H1("Order " + order.getId()));

		// status + meta
		add(new Pre("Status: " + order.getStatus()));

		// items list with thumbnail, modifiers (if any) and qty controls
		VerticalLayout itemsLayout = new VerticalLayout();
		itemsLayout.addClassName("items-list");
		double[] runningTotal = new double[] { 0.0 }; // mutable holder for lambda
		Span totalLabel = new Span("Total: $0.00");
		totalLabel.addClassName("order-total");

		if (order.getItems() != null && !order.getItems().isEmpty()) {
			for (String itemId : order.getItems()) {
				var it = itemService == null ? null : itemService.findById(itemId);
				String title = it == null ? ("Item " + itemId) : it.name();
				double price = it == null ? 0.0 : it.price();
				// per-item qty holder
				int[] qty = new int[] { 1 };

				HorizontalLayout row = new HorizontalLayout();
				row.addClassName("item-row");
				row.setWidthFull();

				// avatar / thumbnail (initials)
				Span avatar = new Span(title.substring(0, Math.min(1, title.length())).toUpperCase());
				avatar.addClassName("item-avatar");
				row.add(avatar);

				VerticalLayout meta = new VerticalLayout();
				meta.setPadding(false);
				meta.setSpacing(false);
				meta.add(new Span(title));
				if (it != null && it.description() != null && !it.description().isBlank()) {
					meta.add(new Span(it.description()));
				}
				row.add(meta);

				// qty controls
				HorizontalLayout qtyControls = new HorizontalLayout();
				Span qtyLabel = new Span(Integer.toString(qty[0]));
				Span linePrice = new Span("$" + String.format("%.2f", price * qty[0]));
				linePrice.addClassName("item-line-price");
				Button minus = new Button("−", evt -> {
					if (qty[0] > 1) {
						qty[0]--;
						qtyLabel.setText(Integer.toString(qty[0]));
						linePrice.setText("$" + String.format("%.2f", price * qty[0]));
						recalculateTotal(itemsLayout, totalLabel);
					}
				});
				Button plus = new Button("+", evt -> {
					qty[0]++;
					qtyLabel.setText(Integer.toString(qty[0]));
					linePrice.setText("$" + String.format("%.2f", price * qty[0]));
					recalculateTotal(itemsLayout, totalLabel);
				});
				minus.getElement().getStyle().set("min-width", "40px");
				plus.getElement().getStyle().set("min-width", "40px");
				qtyControls.add(minus, qtyLabel, plus);
				row.add(qtyControls);

				row.add(linePrice);

				itemsLayout.add(row);
				// accumulate
				runningTotal[0] += price * qty[0];
			}
		}
		// initialize total label with computed running total
		totalLabel.setText("Total: $" + String.format("%.2f", runningTotal[0]));

		// payment controls
		ComboBox<String> paymentMethods = new ComboBox<>("Payment method");
		paymentMethods.setItems("card", "cash", "tap");
		paymentMethods.setValue("card");

		Button requestPayment = new Button("Request Payment", evt -> {
			double amount = parseTotal(totalLabel.getText());
			if (paymentService == null) {
				Notification.show("Payment service unavailable");
				return;
			}
			paymentService.createPayment(order.getId(), amount, paymentMethods.getValue());
			Notification.show("Payment requested for $" + String.format("%.2f", amount));
		});

		Button payNow = new Button("Pay Now", evt -> {
			double amount = parseTotal(totalLabel.getText());
			if (paymentService == null) {
				Notification.show("Payment service unavailable");
				return;
			}
			paymentService.createPayment(order.getId(), amount, paymentMethods.getValue());
			Notification.show("Payment started for $" + String.format("%.2f", amount));
		});

		requestPayment.addClassName("payment-cta");
		payNow.addClassName("payment-cta");

		// action buttons (send/ready) with optimistic undo
		send = new Button("Send to Kitchen", evt -> {
			if (order == null) return;
			String prev = order.getStatus();
			orderService.updateStatus(order.getId(), "IN_PREPARATION");
			Notification n = new Notification();
			n.setDuration(8000);
			Button undo = new Button("Undo", e -> {
				orderService.updateStatus(order.getId(), prev);
				n.close();
			});
			HorizontalLayout content = new HorizontalLayout(new Span("Sent to kitchen"), undo);
			n.add(content);
			n.open();
		});

		ready = new Button("Mark Ready", evt -> {
			if (order == null) return;
			String prev = order.getStatus();
			orderService.updateStatus(order.getId(), "READY");
			Notification n = new Notification();
			n.setDuration(8000);
			Button undo = new Button("Undo", e -> {
				orderService.updateStatus(order.getId(), prev);
				n.close();
			});
			HorizontalLayout content = new HorizontalLayout(new Span("Order marked READY"), undo);
			n.add(content);
			n.open();
		});

		// visibility and permissions remain the same as before
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean hasKitchenRole = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_KITCHEN") || a.getAuthority().equals("ROLE_MANAGEMENT"));
		boolean hasSendRole = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE") || a.getAuthority().equals("ROLE_KITCHEN") || a.getAuthority().equals("ROLE_MANAGEMENT"));
		boolean hasCashierRole = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CASHIER") || a.getAuthority().equals("ROLE_MANAGEMENT"));
		boolean hasCustomerRole = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

		boolean printerAvailable = printerService != null && printJobService != null && templateService != null;
		if (!printerAvailable || !hasSendRole) {
			// hide printers and send button
			printers = new CheckboxGroup<>();
			printers.setVisible(false);
			send.setVisible(false);
		}

		if (!hasKitchenRole) {
			ready.setVisible(false);
		}

		paymentMethods.setVisible(hasCashierRole || hasCustomerRole);
		requestPayment.setVisible(hasCashierRole);
		payNow.setVisible(hasCustomerRole);

		HorizontalLayout actions = new HorizontalLayout();
		actions.add(send, ready);
		if (paymentMethods.isVisible()) actions.add(paymentMethods);
		if (requestPayment.isVisible()) actions.add(requestPayment);
		if (payNow.isVisible()) actions.add(payNow);

		add(itemsLayout, totalLabel, actions);
	}

	private double parseTotal(String totalText) {
		try {
			return Double.parseDouble(totalText.replace("Total: $", "").replace(",", "").trim());
		} catch (NumberFormatException e) {
			return 0.0; // Fallback to 0 if parsing fails
		}
	}

	// sum item-line-price spans inside given layout and update the totalLabel
	private void recalculateTotal(VerticalLayout itemsLayout, Span totalLabel) {
		double sum = 0.0;
		for (com.vaadin.flow.component.Component c : itemsLayout.getChildren().toList()) {
			if (c instanceof HorizontalLayout hl) {
				for (com.vaadin.flow.component.Component ch : hl.getChildren().toList()) {
					if (ch instanceof Span sp && sp.getElement().getClassList().contains("item-line-price")) {
						try {
							sum += Double.parseDouble(sp.getText().replace("$", ""));
						} catch (Exception ignored) {}
					}
				}
			}
		}
		totalLabel.setText("Total: $" + String.format("%.2f", sum));
	}
} 
package com.possable.order.ui;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.possable.order.OrderFacade;
import com.possable.inventory.InventoryFacade;
import com.possable.checkout.CheckoutFacade;
import com.possable.print.PrintFacade;
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

	private final OrderFacade orderFacade;
	private final PrintFacade printFacade;
	private CheckboxGroup<String> printers;
	private Button send;
	private Button ready;
	private final InventoryFacade inventoryFacade;
	private final CheckoutFacade checkoutFacade;

	public OrdersDetailComponent(OrderFacade orderFacade, PrintFacade printFacade, InventoryFacade inventoryFacade, CheckoutFacade checkoutFacade) {
		this.orderFacade = orderFacade;
		this.printFacade = printFacade;
		this.inventoryFacade = inventoryFacade;
		this.checkoutFacade = checkoutFacade;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-details");
	}

	@SuppressWarnings("unused")
	public void showOrder(com.possable.order.OrderFacade.OrderInfo order) {
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
				var it = inventoryFacade == null ? null : inventoryFacade.findById(itemId);
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
			if (checkoutFacade == null) {
				Notification.show("Payment service unavailable");
				return;
			}
			checkoutFacade.createPayment(order.getId(), amount, paymentMethods.getValue());
			Notification.show("Payment requested for $" + String.format("%.2f", amount));
		});

		Button payNow = new Button("Pay Now", evt -> {
			double amount = parseTotal(totalLabel.getText());
			if (checkoutFacade == null) {
				Notification.show("Payment service unavailable");
				return;
			}
			checkoutFacade.createPayment(order.getId(), amount, paymentMethods.getValue());
			Notification.show("Payment started for $" + String.format("%.2f", amount));
		});

		requestPayment.addClassName("payment-cta");
		payNow.addClassName("payment-cta");

		// action buttons (send/ready) with optimistic undo
		send = new Button("Send to Kitchen", evt -> {
			if (order == null) return;
			String prev = order.getStatus();
			orderFacade.updateStatus(order.getId(), "IN_PREPARATION");
			Notification n = new Notification();
			n.setDuration(8000);
			Button undo = new Button("Undo", e -> {
				orderFacade.updateStatus(order.getId(), prev);
				n.close();
			});
			HorizontalLayout content = new HorizontalLayout(new Span("Sent to kitchen"), undo);
			n.add(content);
			n.open();
		});

		ready = new Button("Mark Ready", evt -> {
			if (order == null) return;
			String prev = order.getStatus();
			orderFacade.updateStatus(order.getId(), "READY");
			Notification n = new Notification();
			n.setDuration(8000);
			Button undo = new Button("Undo", e -> {
				orderFacade.updateStatus(order.getId(), prev);
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

		boolean printerAvailable = printFacade != null;
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

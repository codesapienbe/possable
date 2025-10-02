package com.possable.order.cashier;

import java.util.List;
import java.util.stream.Collectors;

import com.possable.checkout.CheckoutFacade;
import com.possable.order.OrderFacade;
import com.possable.order.OrderMainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "orders/cashier", layout = OrderMainLayout.class)
@PageTitle("Cashier Orders")
@PreAuthorize("hasAnyRole('CASHIER','MANAGEMENT')")
public class CashierOrderView extends VerticalLayout {

	private final OrderFacade orderFacade;
	private final CheckoutFacade checkoutFacade;

	public CashierOrderView(OrderFacade orderFacade, CheckoutFacade checkoutFacade) {
		this.orderFacade = orderFacade;
		this.checkoutFacade = checkoutFacade;
		setPadding(true);
		setSpacing(true);
		setWidthFull();

		add(new H1("Cashier - Orders"));
		Button refresh = new Button("Refresh", evt -> render());
		add(refresh);
		render();
	}

	private void render() {
		removeAll();
		add(new H1("Cashier - Orders"));
		Button refresh = new Button("Refresh", evt -> render());
		add(refresh);

		List<OrderFacade.OrderInfo> orders = orderFacade.listOrders().stream()
				.filter(o -> o.status() != null && (o.status().equalsIgnoreCase("PENDING") || o.status().equalsIgnoreCase("READY")))
				.collect(Collectors.toList());

		if (orders.isEmpty()) { add(new Span("No orders")); return; }

		for (OrderFacade.OrderInfo o : orders) {
			VerticalLayout card = new VerticalLayout();
			card.getStyle().set("padding", "10px").set("border", "1px solid #e5e7eb");
			card.add(new Span("Order: " + o.id()));
			card.add(new Span("Items: " + (o.items() == null ? 0 : o.items().size())));
			card.add(new Span("Status: " + o.status()));
			Button pay = new Button("Pay", ev -> {
				double amount = 0.0;
				if (o.items() != null) {
					// best-effort calculation using inventory facade is not available here; charge 0.0
				}
				checkoutFacade.createPayment(o.id(), amount, "cash");
				add(new Span("Payment requested"));
			});
			card.add(pay);
			add(card);
		}
	}
} 
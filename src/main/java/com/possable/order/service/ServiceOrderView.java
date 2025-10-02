package com.possable.order.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.possable.order.OrderFacade;
import com.possable.print.PrintFacade;
import com.possable.order.OrderMainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "orders/service", layout = OrderMainLayout.class)
@PageTitle("Service Orders")
@PreAuthorize("hasAnyRole('SERVICE','MANAGEMENT')")
public class ServiceOrderView extends VerticalLayout {

	private final OrderFacade orderFacade;
	private final PrintFacade printFacade;

	public ServiceOrderView(OrderFacade orderFacade, PrintFacade printFacade) {
		this.orderFacade = orderFacade;
		this.printFacade = printFacade;
		setPadding(true);
		setSpacing(true);
		setWidthFull();

		add(new H1("Service - Orders"));

		Button refresh = new Button("Refresh", evt -> render());
		add(refresh);

		render();
	}

	private void render() {
		removeAll();
		add(new H1("Service - Orders"));
		Button refresh = new Button("Refresh", evt -> render());
		add(refresh);

		List<OrderFacade.OrderInfo> orders = orderFacade.listOrders().stream()
				.filter(o -> o.status() != null && (o.status().equalsIgnoreCase("PENDING") || o.status().equalsIgnoreCase("IN_PREPARATION")))
				.collect(Collectors.toList());

		if (orders.isEmpty()) { add(new Span("No orders")); return; }

		for (OrderFacade.OrderInfo o : orders) {
			VerticalLayout card = new VerticalLayout();
			card.getStyle().set("padding", "10px").set("border", "1px solid #e5e7eb");
			card.add(new Span("Order: " + o.id()));
			card.add(new Span("Items: " + (o.items() == null ? 0 : o.items().size())));
			card.add(new Span("Status: " + o.status()));

			Button sendToKitchen = new Button("Send to Kitchen", ev -> {
				var printers = printFacade.listPrinters(Map.of());
				var templates = printFacade.listTemplates(Map.of());
				int created = 0;
				for (var p : printers) {
					var tpl = templates.stream().filter(t -> t.printerCategory().equals(p.category())).findFirst();
					if (tpl.isPresent()) { printFacade.createJob(o.id(), p.id(), tpl.get().id()); created++; }
				}
				orderFacade.updateStatus(o.id(), "IN_PREPARATION");
				render();
				add(new Span("Print jobs created: " + created));
			});
			card.add(sendToKitchen);
			add(card);
		}
	}
} 
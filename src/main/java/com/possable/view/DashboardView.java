package com.possable.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("POS Dashboard")
public class DashboardView extends VerticalLayout {

	public DashboardView() {
		setPadding(true);
		setSpacing(true);
		setWidthFull();

		H1 header = new H1("Dashboard");
		add(header);

		HorizontalLayout stats = new HorizontalLayout();
		stats.setWidthFull();
		stats.getStyle().set("gap", "var(--lumo-space-m)");
		stats.add(createStat("Orders", "0"), createStat("Revenue", "$0.00"), createStat("Printers", "0"));
		add(stats);
	}

	private Component createStat(String label, String value) {
		VerticalLayout card = new VerticalLayout();
		card.getStyle().set("padding", "var(--lumo-space-m)").set("border", "1px solid var(--lumo-contrast-10pct)").set("border-radius", "8px");
		card.add(new H3(label), new Span(value));
		return card;
	}
} 
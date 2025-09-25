package com.possable.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.html.Div;

import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "kitchen", layout = MainLayout.class)
@PageTitle("Kitchen")
@PreAuthorize("hasAnyRole('KITCHEN','MANAGEMENT')")
public class KitchenView extends VerticalLayout {

	public KitchenView(RoleDashboardFactory factory) {
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Kitchen"));

		Tabs tabs = new Tabs();
		Tab ordersTab = new Tab("Orders");
		tabs.add(ordersTab);

		Div content = new Div();
		content.add(factory.createOrdersComponent());
		add(tabs, content);
	}
} 
package com.possable.view;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "service", layout = MainLayout.class)
@PageTitle("Service")
@PreAuthorize("hasAnyRole('SERVICE','MANAGEMENT')")
public class ServiceView extends VerticalLayout {

	public ServiceView(RoleDashboardFactory factory) {
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Service"));

		Tabs tabs = new Tabs();
		Tab menuTab = new Tab("Menu");
		Tab ordersTab = new Tab("Orders");
		tabs.add(menuTab, ordersTab);

		Div content = new Div();
		content.setWidthFull();

		Map<Tab, Component> tabToContent = new HashMap<>();
		tabToContent.put(menuTab, factory.createItemListComponent());
		tabToContent.put(ordersTab, factory.createOrdersComponent());

		tabs.addSelectedChangeListener(ev -> {
			content.removeAll();
			content.add(tabToContent.get(ev.getSelectedTab()));
		});

		add(tabs, content);
		// select first
		tabs.setSelectedTab(menuTab);
		content.add(tabToContent.get(menuTab));
	}
} 
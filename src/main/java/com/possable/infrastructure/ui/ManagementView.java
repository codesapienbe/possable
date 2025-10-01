package com.possable.infrastructure.ui;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.security.access.prepost.PreAuthorize;
import com.possable.management.ManagementDashboardFactory;
import com.possable.management.ui.LogViewerComponent;

@Route(value = "management", layout = com.possable.user.UserMainLayout.class)
@PageTitle("Management")
@PreAuthorize("hasRole('MANAGEMENT')")
public class ManagementView extends VerticalLayout {

	public ManagementView(ManagementDashboardFactory factory) {
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Management"));

		Tabs tabs = new Tabs();
		Tab itemsTab = new Tab("Items");
		Tab ordersTab = new Tab("Orders");
		Tab printersTab = new Tab("Printers");
		Tab jobsTab = new Tab("Print Jobs");
		Tab logsTab = new Tab("Logs");
		tabs.add(itemsTab, ordersTab, printersTab, jobsTab, logsTab);

		Map<Tab, Component> map = new HashMap<>();
		map.put(itemsTab, factory.createItemListComponent());
		map.put(ordersTab, factory.createOrdersComponent());
		map.put(printersTab, factory.createPrintersComponent());
		map.put(jobsTab, factory.createPrintJobsComponent());
		map.put(logsTab, new LogViewerComponent());

		Div content = new Div();
		content.setWidthFull();
		content.add(map.get(itemsTab));

		tabs.addSelectedChangeListener(ev -> {
			content.removeAll();
			content.add(map.get(ev.getSelectedTab()));
		});

		add(tabs, content);
	}

} 

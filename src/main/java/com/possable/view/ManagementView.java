package com.possable.view;

import java.util.HashMap;
import java.util.Map;

import com.possable.service.ItemService;
import com.possable.service.OrderService;
import com.possable.service.PrinterService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "management", layout = MainLayout.class)
@PageTitle("Management")
@PreAuthorize("hasRole('MANAGEMENT')")
public class ManagementView extends VerticalLayout {

	public ManagementView(ItemService itemService, OrderService orderService, PrinterService printerService, PrintJobService printJobService, PrintTemplateService templateService) {
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
		tabs.add(itemsTab, ordersTab, printersTab, jobsTab);

		Map<Tab, Component> map = new HashMap<>();
		map.put(itemsTab, new ItemListComponent(itemService));
		map.put(ordersTab, new OrdersComponent(orderService, printerService, printJobService, templateService));
		map.put(printersTab, new PrintersComponent(printerService));
		map.put(jobsTab, new PrintJobsComponent(printJobService));

		com.vaadin.flow.component.html.Div content = new com.vaadin.flow.component.html.Div();
		content.setWidthFull();
		content.add(map.get(itemsTab));

		tabs.addSelectedChangeListener(ev -> {
			content.removeAll();
			content.add(map.get(ev.getSelectedTab()));
		});

		add(tabs, content);
	}

} 
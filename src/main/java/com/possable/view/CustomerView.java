package com.possable.view;

import com.possable.service.ItemService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "customer", layout = MainLayout.class)
@PageTitle("Customer")
@PreAuthorize("permitAll()")
public class CustomerView extends VerticalLayout {

	private final ItemService itemService;

	public CustomerView(ItemService itemService) {
		this.itemService = itemService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Customer Menu"));

		add(new ItemListComponent(itemService));
	}
} 
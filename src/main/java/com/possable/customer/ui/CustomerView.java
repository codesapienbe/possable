package com.possable.customer.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "customer", layout = com.possable.customer.CustomerMainLayout.class)
@PageTitle("Customer")
@PreAuthorize("permitAll()")
public class CustomerView extends VerticalLayout {

	public CustomerView(com.possable.management.ManagementDashboardFactory factory) {
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Customer Menu"));

		add(factory.createItemListComponent());
	}
} 


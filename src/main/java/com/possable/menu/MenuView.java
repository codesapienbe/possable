package com.possable.menu;

import com.possable.infrastructure.ui.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "menu", layout = MainLayout.class)
@PageTitle("Menu")
public class MenuView extends VerticalLayout {

	public MenuView() {
		setWidthFull();
		setPadding(true);
		add(new H2("Menu (Products)"));
		add(new Paragraph("Placeholder for product/menu module. Items will be listed here."));
	}
} 
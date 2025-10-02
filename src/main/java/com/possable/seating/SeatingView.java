package com.possable.seating;

import com.possable.infrastructure.ui.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "seating", layout = MainLayout.class)
@PageTitle("Seating")
public class SeatingView extends VerticalLayout {

	public SeatingView() {
		setWidthFull();
		setPadding(true);
		add(new H2("Seating Plan"));
		add(new Paragraph("Placeholder for seating module. Will show table layout and floor plan."));
	}
} 
package com.possable.infrastructure.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "access-denied", layout = MainLayout.class)
@PageTitle("Access Denied")
public class AccessDeniedView extends VerticalLayout {

	public AccessDeniedView() {
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		H1 header = new H1("Access Denied");
		header.getStyle().set("font-size", "1.6em");
		add(header);

		Span msg = new Span("You do not have permission to view this page.");
		msg.getStyle().set("display", "block").set("margin", "12px 0 18px 0");
		add(msg);

		Button back = new Button("Back to Entry", e -> getUI().ifPresent(ui -> ui.navigate(EntryPointView.class)));
		back.addClassName("pos-button-large");
		add(new HorizontalLayout(back));
	}
} 

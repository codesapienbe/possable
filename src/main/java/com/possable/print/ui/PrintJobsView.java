package com.possable.print.ui;

import java.util.List;

import com.possable.print.PrintFacade;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.possable.service.Broadcaster;

@Route(value = "print-jobs-ui", layout = MainLayout.class)
@PageTitle("Print Jobs")
public class PrintJobsView extends VerticalLayout {

	public PrintJobsView(PrintFacade printFacade) {
		add(new PrintJobsComponent(printFacade));
	}
} 


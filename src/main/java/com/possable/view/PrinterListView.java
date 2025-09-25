package com.possable.view;

import com.possable.service.PrinterService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "printers", layout = MainLayout.class)
@PageTitle("Printers")
public class PrinterListView extends VerticalLayout {

	private final PrinterService printerService;
	private final Grid<PrinterService.Printer> grid = new Grid<>(PrinterService.Printer.class, false);
	private final com.vaadin.flow.component.orderedlayout.HorizontalLayout skeletonTiles = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();

	public PrinterListView(PrinterService printerService) {
		this.printerService = printerService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Printers"));

		grid.addColumn(PrinterService.Printer::id).setHeader("ID").setAutoWidth(true);
		grid.addColumn(PrinterService.Printer::name).setHeader("Name").setAutoWidth(true);
		grid.addColumn(PrinterService.Printer::category).setHeader("Category").setAutoWidth(true);
		grid.addColumn(PrinterService.Printer::description).setHeader("Description").setAutoWidth(true);
		grid.setItems(printerService.listPrinters());
		// skeleton tiles
		skeletonTiles.setClassName("skeleton-tiles-container");
		for (int i = 0; i < 3; i++) {
			com.vaadin.flow.component.html.Div s = new com.vaadin.flow.component.html.Div();
			s.addClassName("skeleton-tile");
			skeletonTiles.add(s);
		}
		add(skeletonTiles, grid);

		// Register form
		TextField name = new TextField("Name");
		TextField category = new TextField("Category");
		TextArea description = new TextArea("Description");
		description.setWidthFull();
		name.setPlaceholder("e.g. Kitchen Printer");
		category.setPlaceholder("e.g. kitchen, bar, receipt");

		Button register = new Button("Add Printer", evt -> {
			if (name.isEmpty() || category.isEmpty()) {
				Notification.show("Name and category are required", 3000, Notification.Position.TOP_END);
				return;
			}
			var p = printerService.registerPrinter(name.getValue(), category.getValue(), description.getValue());
			Notification.show("Registered printer: " + p.name(), 3000, Notification.Position.TOP_END);
			refresh();
			name.clear();
			category.clear();
			description.clear();
		});
		register.setIcon(new Icon(VaadinIcon.PLUS));
		register.addClassName("pos-button-large");

		HorizontalLayout form = new HorizontalLayout(name, category, register);
		form.setWidthFull();
		add(form);

		addAttachListener(evt -> refresh());
	}

	private void refresh() {
		// show skeletons while refreshing
		skeletonTiles.setVisible(true);
		grid.setVisible(false);
		grid.setItems(printerService.listPrinters());
		skeletonTiles.setVisible(false);
		grid.setVisible(true);
	}
} 
package com.possable.view;

import java.util.List;

import com.possable.service.PrintJobService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "print-jobs-ui", layout = MainLayout.class)
@PageTitle("Print Jobs")
public class PrintJobsView extends VerticalLayout {

	private final PrintJobService jobService;
	private final Grid<PrintJobService.PrintJob> grid = new Grid<>(PrintJobService.PrintJob.class, false);

	public PrintJobsView(PrintJobService jobService) {
		this.jobService = jobService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H1("Print Jobs"));

		grid.addColumn(PrintJobService.PrintJob::id).setHeader("ID").setAutoWidth(true);
		grid.addColumn(PrintJobService.PrintJob::orderId).setHeader("Order ID").setAutoWidth(true);
		grid.addColumn(PrintJobService.PrintJob::printerId).setHeader("Printer ID").setAutoWidth(true);
		grid.addColumn(PrintJobService.PrintJob::templateId).setHeader("Template ID").setAutoWidth(true);
		grid.addColumn(PrintJobService.PrintJob::status).setHeader("Status").setAutoWidth(true);
		grid.addColumn(p -> p.createdAt().toString()).setHeader("Created At").setAutoWidth(true);

		refreshGrid();
		add(grid);

		Button refresh = new Button("Refresh", evt -> refreshGrid());
		Button markCompleted = new Button("Mark Completed", evt -> updateSelectedStatus("completed"));
		Button markFailed = new Button("Mark Failed", evt -> updateSelectedStatus("failed"));
		HorizontalLayout actions = new HorizontalLayout(refresh, markCompleted, markFailed);
		add(actions);
	}

	private void refreshGrid() {
		List<PrintJobService.PrintJob> jobs = jobService.listJobs(null);
		grid.setItems(jobs);
	}

	private void updateSelectedStatus(String status) {
		var opt = grid.getSelectionModel().getFirstSelectedItem();
		if (opt == null || opt.isEmpty()) {
			Notification.show("Select a print job first", 3000, Notification.Position.TOP_END);
			return;
		}
		var selected = opt.get();
		jobService.updateStatus(selected.id(), status);
		Notification.show("Updated print job status to " + status, 3000, Notification.Position.TOP_END);
		refreshGrid();
	}
} 
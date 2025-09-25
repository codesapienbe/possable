package com.possable.view;

import java.util.List;

import com.possable.service.PrintJobService;
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
import com.possable.service.Broadcaster;
import com.vaadin.flow.shared.Registration;

public class PrintJobsComponent extends VerticalLayout {

	private final PrintJobService jobService;
	private final Grid<PrintJobService.PrintJob> grid = new Grid<>(PrintJobService.PrintJob.class, false);
	private Registration broadcasterRegistration;
	private Span badge = new Span("");
	private int badgeCount = 0;

	public PrintJobsComponent(PrintJobService jobService) {
		this.jobService = jobService;
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		HorizontalLayout headerLine = new HorizontalLayout();
		headerLine.setWidthFull();
		H1 title = new H1("Print Queue");
		title.getStyle().set("font-size", "1.3em").set("font-weight", "600");
		badge.getStyle().set("background", "var(--pos-accent)").set("color", "#061017").set("padding", "4px 8px").set("border-radius", "12px").set("margin-left", "8px");
		badge.setVisible(false);
		headerLine.add(title, badge);
		add(headerLine);

		grid.addColumn(PrintJobService.PrintJob::id).setHeader("ID").setAutoWidth(true);
		grid.addColumn(PrintJobService.PrintJob::orderId).setHeader("Order ID").setAutoWidth(true);
		grid.addColumn(PrintJobService.PrintJob::printerId).setHeader("Printer ID").setAutoWidth(true);
		grid.addColumn(PrintJobService.PrintJob::templateId).setHeader("Template ID").setAutoWidth(true);
		grid.addColumn(PrintJobService.PrintJob::status).setHeader("Status").setAutoWidth(true);
		grid.addColumn(p -> p.createdAt().toString()).setHeader("Created At").setAutoWidth(true);

		refreshGrid();

		add(grid);

		Button refresh = new Button("Refresh", evt -> refreshGrid());
		refresh.setIcon(new Icon(VaadinIcon.REFRESH));
		refresh.addClassName("pos-button-large");

		Button markCompleted = new Button("Mark Completed", evt -> updateSelectedStatus("completed"));
		markCompleted.setIcon(new Icon(VaadinIcon.CHECK));
		markCompleted.addClassName("pos-button-large");

		Button markFailed = new Button("Mark Failed", evt -> updateSelectedStatus("failed"));
		markFailed.setIcon(new Icon(VaadinIcon.CLOSE_SMALL));
		markFailed.addClassName("pos-button-large");

		HorizontalLayout actions = new HorizontalLayout(refresh, markCompleted, markFailed);
		add(actions);

		addAttachListener(evt -> {
			refreshGrid();
			// register for real-time updates via Broadcaster (no polling)
			broadcasterRegistration = Broadcaster.register(msg -> getUI().ifPresent(ui -> ui.access(() -> {
				// on new event, refresh and show badge/toast
				refreshGrid();
				showNewEventNotification();
			}))); 
		});

		addDetachListener(evt -> {
			if (broadcasterRegistration != null) {
				broadcasterRegistration.remove();
				broadcasterRegistration = null;
			}
		});
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

	private void showNewEventNotification() {
		badgeCount++;
		badge.setText(Integer.toString(badgeCount));
		badge.setVisible(true);
		badge.getElement().getClassList().add("pulse-badge");
		UI ui = UI.getCurrent();
		if (ui != null) {
			new Thread(() -> {
				try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
				ui.access(() -> badge.getElement().getClassList().remove("pulse-badge"));
			}).start();
		}
		Notification.show("New print job event received", 1500, Notification.Position.TOP_END);
	}
} 
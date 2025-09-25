package com.possable.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.possable.service.DemoNotificationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

	private static final Logger log = LoggerFactory.getLogger(MainLayout.class);

	private final DemoNotificationService demoNotificationService;

	public MainLayout(DemoNotificationService demoNotificationService) {
		this.demoNotificationService = demoNotificationService;

		H1 title = new H1("Possable POS");
		title.getStyle().set("margin", "0").set("font-size", "18px");

		Tabs menu = new Tabs();
		menu.setOrientation(Tabs.Orientation.HORIZONTAL);
		menu.add(createTab("Dashboard", DashboardView.class), createTab("Items", ItemListView.class), createTab("Orders", OrderView.class));
		menu.getStyle().set("min-width", "480px");

		Span status = new Span("Connected");
		status.getStyle().set("margin-left", "var(--lumo-space-m)");
		status.getElement().getThemeList().add("badge success");

		Span user = new Span("Waiter");
		user.getStyle().set("margin-left", "auto");

		HorizontalLayout header = new HorizontalLayout(title, menu, status, user);
		header.setWidthFull();
		header.setAlignItems(Alignment.CENTER);
		header.expand(menu);
		// add CSS class names for POS theme hooks
		header.addClassName("pos-header");
		menu.addClassName("pos-menu");
		status.addClassName("pos-status");
		user.addClassName("pos-user");
		addClassName("pos-app");
		addToNavbar(header);

		addAttachListener(evt -> {
			String msg = this.demoNotificationService.consumeStartupMessage();
			if (msg != null && !msg.isBlank()) {
				Notification.show(msg, 5000, Notification.Position.TOP_END);
			}
		});
	}

	private Tab createTab(String text, Class<? extends Component> navigationTarget) {
		RouterLink link = new RouterLink(text, navigationTarget);
		Tab tab = new Tab(link);
		return tab;
	}
} 
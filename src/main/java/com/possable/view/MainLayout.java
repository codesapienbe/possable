package com.possable.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.possable.service.DemoNotificationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.AfterNavigationEvent;
import java.util.Map;
import java.util.HashMap;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class MainLayout extends AppLayout implements AfterNavigationObserver {

	private static final Logger log = LoggerFactory.getLogger(MainLayout.class);

	private final DemoNotificationService demoNotificationService;

	@Value("${app.auth.inactivity-timeout-ms:300000}")
	private long inactivityTimeoutMs; // milliseconds
	private static final ScheduledExecutorService INACTIVITY_SCHED = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "inactivity-scheduler");
		t.setDaemon(true);
		return t;
	});
	private ScheduledFuture<?> inactivityFuture;

	// keep a persistent Tabs instance and route->Tab mapping so we can synchronize selection reliably
	private final Tabs menu = new Tabs();
	private final Map<String, Tab> routeToTab = new HashMap<>();
	private final Map<Tab, Class<? extends Component>> tabToTarget = new HashMap<>();

	public MainLayout(DemoNotificationService demoNotificationService) {
		this.demoNotificationService = demoNotificationService;

		H1 title = new H1("Possable POS");
		title.getStyle().set("margin", "0").set("font-size", "18px");

		menu.setOrientation(Tabs.Orientation.HORIZONTAL);
		menu.add(createTab("Dashboard", DashboardView.class), createTab("Items", ItemListView.class), createTab("Orders", OrderView.class), createTab("Printers", PrinterListView.class), createTab("Print Jobs", PrintJobsView.class));
		// when a tab is selected, navigate to its target
		menu.addSelectedChangeListener(ev -> {
			Tab s = ev.getSelectedTab();
			if (s != null) {
				Class<? extends Component> target = tabToTarget.get(s);
				if (target != null) {
					getUI().ifPresent(u -> u.navigate(target));
				}
			}
		});
		menu.getStyle().set("min-width", "480px");

		Span status = new Span("Connected");
		status.getStyle().set("margin-left", "var(--lumo-space-m)");
		status.getElement().getThemeList().add("badge success");

		// mobile menu toggle (hidden on desktop via CSS)
		Button menuToggle = new Button(new Icon(VaadinIcon.MENU));
		menuToggle.addClassName("menu-toggle");
		menuToggle.addClickListener(e -> {
			Dialog mobile = new Dialog();
			// accessibility attributes for the dialog
			mobile.getElement().setAttribute("role", "dialog");
			mobile.getElement().setAttribute("aria-label", "Main navigation");
			mobile.getElement().setAttribute("aria-modal", "true");
			VerticalLayout content = new VerticalLayout();
			content.getElement().setAttribute("role", "menu");
			RouterLink l1 = new RouterLink("Dashboard", DashboardView.class);
			l1.getElement().setAttribute("role", "menuitem");
			l1.getElement().setAttribute("aria-label", "Navigate to Dashboard");
			RouterLink l2 = new RouterLink("Items", ItemListView.class);
			l2.getElement().setAttribute("role", "menuitem");
			l2.getElement().setAttribute("aria-label", "Navigate to Items");
			RouterLink l3 = new RouterLink("Orders", OrderView.class);
			l3.getElement().setAttribute("role", "menuitem");
			l3.getElement().setAttribute("aria-label", "Navigate to Orders");
			RouterLink l4 = new RouterLink("Printers", PrinterListView.class);
			l4.getElement().setAttribute("role", "menuitem");
			l4.getElement().setAttribute("aria-label", "Navigate to Printers");
			RouterLink l5 = new RouterLink("Print Jobs", PrintJobsView.class);
			l5.getElement().setAttribute("role", "menuitem");
			l5.getElement().setAttribute("aria-label", "Navigate to Print Jobs");
			content.add(l1, l2, l3, l4, l5);
			mobile.add(content);
			mobile.setWidth("280px");
			mobile.open();
		});

		Span user = new Span("");
		user.getStyle().set("margin-left", "auto");

		Span roleBadge = new Span("");
		roleBadge.getStyle().set("margin-left", "8px");
		roleBadge.getElement().getThemeList().add("badge");
		roleBadge.addClassName("pos-role-badge");

		Button logout = new Button("Logout", evt -> {
			Dialog confirm = new Dialog();
			confirm.add(new Span("Are you sure you want to logout?"));
			Button yes = new Button("Logout", e -> {
				SecurityContextHolder.clearContext();
				confirm.close();
				getUI().ifPresent(ui -> {
					ui.navigate(com.possable.view.EntryPointView.class);
					ui.getPage().reload();
				});
			});
			Button no = new Button("Cancel", e -> confirm.close());
			yes.addClassName("pos-button-large");
			no.addClassName("pos-button-large");
			confirm.add(new HorizontalLayout(yes, no));
			confirm.open();
		});
		logout.addClassName("pos-button-large");

		HorizontalLayout header = new HorizontalLayout(title, menuToggle, menu, status, user, roleBadge, logout);
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
			// show startup message if present
			String msg = this.demoNotificationService.consumeStartupMessage();
			if (msg != null && !msg.isBlank()) {
				Notification.show(msg, 5000, Notification.Position.TOP_END);
			}
			// update user display and role badge from security context
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
				user.setText(auth.getName());
				var authorities = auth.getAuthorities();
				if (authorities != null && !authorities.isEmpty()) {
					var rolesList = authorities.stream().map(a -> a.getAuthority().replace("ROLE_", "")).toList();
					String roles = String.join(", ", rolesList);
					roleBadge.setText(roles);
					// set role-specific classes for styling
					roleBadge.getElement().getClassList().remove("role-service");
					roleBadge.getElement().getClassList().remove("role-kitchen");
					roleBadge.getElement().getClassList().remove("role-management");
					for (String r : rolesList) {
						if ("SERVICE".equalsIgnoreCase(r)) roleBadge.getElement().getClassList().add("role-service");
						if ("KITCHEN".equalsIgnoreCase(r)) roleBadge.getElement().getClassList().add("role-kitchen");
						if ("MANAGEMENT".equalsIgnoreCase(r)) roleBadge.getElement().getClassList().add("role-management");
					}
				} else {
					roleBadge.setText("");
				}
			} else {
				user.setText("");
				roleBadge.setText("");
			}

			// register client-side listeners to report activity to server
			getElement().executeJs("const root=$0; function __vaadin_report(){ try{root.$server.reportActivity();}catch(e){} } window.addEventListener('mousemove', __vaadin_report); window.addEventListener('keydown', __vaadin_report); window.addEventListener('touchstart', __vaadin_report);", getElement());

			// start inactivity checker for this UI
			var ui = evt.getUI();
			if (inactivityFuture != null) {
				inactivityFuture.cancel(false);
				inactivityFuture = null;
			}
			inactivityFuture = INACTIVITY_SCHED.scheduleAtFixedRate(() -> {
				ui.access(() -> {
					Authentication a = SecurityContextHolder.getContext().getAuthentication();
					if (a == null || !a.isAuthenticated()) return;
					Object last = VaadinSession.getCurrent().getAttribute("lastActivity");
					long lastMs = last instanceof Long ? (Long) last : System.currentTimeMillis();
					if (System.currentTimeMillis() - lastMs > inactivityTimeoutMs) {
						SecurityContextHolder.clearContext();
						Notification.show("Logged out due to inactivity", 3000, Notification.Position.TOP_END);
						ui.navigate(com.possable.view.EntryPointView.class);
						ui.getPage().reload();
					}
				});
			}, 30, 30, TimeUnit.SECONDS);
		});

		addDetachListener(evt -> {
			if (inactivityFuture != null) {
				inactivityFuture.cancel(false);
				inactivityFuture = null;
			}
		});

	}

	@ClientCallable
	public void reportActivity() {
		// called from client-side JS when user interacts
		VaadinSession session = VaadinSession.getCurrent();
		if (session != null) {
			session.setAttribute("lastActivity", System.currentTimeMillis());
		}
	}

	private Tab createTab(String text, Class<? extends Component> navigationTarget) {
		RouterLink link = new RouterLink(text, navigationTarget);
		Tab tab = new Tab(link);
		// determine route path from @Route annotation if present
		var ann = navigationTarget.getAnnotation(com.vaadin.flow.router.Route.class);
		String routePath = (ann != null && ann.value() != null && !ann.value().isBlank()) ? ann.value() : "";
		routeToTab.put(routePath, tab);
		tabToTarget.put(tab, navigationTarget);
		return tab;
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		String first = event.getLocation().getFirstSegment();
		String route = first == null ? "" : first;
		Tab t = routeToTab.get(route);
		if (t != null) {
			menu.setSelectedTab(t);
		} else {
			// clear selection when route doesn't match
			menu.setSelectedTab(null);
		}
	}

} 
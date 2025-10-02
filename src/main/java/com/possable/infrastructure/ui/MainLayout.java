package com.possable.infrastructure.ui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.possable.notification.NotificationFacade;
import com.possable.infrastructure.Broadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.infrastructure.ui.EntryPointView;
import com.possable.user.ui.ProfileView;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import java.util.Map;

public class MainLayout extends AppLayout {

	private static final Logger log = LoggerFactory.getLogger(MainLayout.class);

	private final NotificationFacade notificationFacade;

	@Value("${app.auth.inactivity-timeout-ms:300000}")
	private long inactivityTimeoutMs; // milliseconds
	private static final ScheduledExecutorService INACTIVITY_SCHED = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "inactivity-scheduler");
		t.setDaemon(true);
		return t;
	});
	private ScheduledFuture<?> inactivityFuture;
	private Registration broadcasterRegistration;

	public MainLayout(NotificationFacade notificationFacade) {
		this.notificationFacade = notificationFacade;

		H1 title = new H1("Possable POS");
		title.getStyle().set("margin", "0").set("font-size", "18px");

		Span status = new Span("Connected");
		status.getStyle().set("margin-left", "var(--lumo-space-m)");
		status.getElement().getThemeList().add("badge success");

		// Profile icon button (icon-only) that navigates to profile page
		Button profile = new Button(VaadinIcon.USER.create(), e -> getUI().ifPresent(ui -> ui.navigate(ProfileView.class)));
		profile.addClassName("pos-button-icon");
		profile.getElement().setAttribute("title", "Not signed in");
		// hide profile by default; will be made visible when user is authenticated
		profile.setVisible(false);

		Button home = new Button(VaadinIcon.HOME.create(), e -> getUI().ifPresent(ui -> ui.navigate(EntryPointView.class)));
		home.addClassName("pos-button-large");

		HorizontalLayout header = new HorizontalLayout(title, status, profile, home);
		header.setWidthFull();
		header.setAlignItems(Alignment.CENTER);
		header.addClassName("pos-header");
		status.addClassName("pos-status");
		addClassName("pos-app");
		addToNavbar(header);

		// Left drawer menu (role-aware)
		com.vaadin.flow.component.orderedlayout.VerticalLayout drawer = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
		drawer.getStyle().set("padding", "12px").set("min-width", "180px");
		drawer.addClassName("pos-drawer");

		Authentication authNow = SecurityContextHolder.getContext().getAuthentication();
		boolean unauthenticated = (authNow == null || !authNow.isAuthenticated());
		Set<String> roleNames = new HashSet<>();
		if (!unauthenticated) {
			for (GrantedAuthority ga : authNow.getAuthorities()) {
				String a = ga.getAuthority();
				if (a != null) {
					if (a.startsWith("ROLE_")) a = a.substring(5);
					roleNames.add(a.toUpperCase());
				}
			}
		}

		// Unauthenticated (customer) sees Menu if available
		if (unauthenticated) {
			try { Class.forName("com.possable.menu.MenuView"); drawer.add(new com.vaadin.flow.router.RouterLink("Menu", com.possable.menu.MenuView.class)); } catch (ClassNotFoundException ignore) {}
		} else {
			boolean isService = roleNames.contains("SERVICE");
			boolean isKitchen = roleNames.contains("KITCHEN");
			boolean isManagement = roleNames.contains("MANAGEMENT");
			boolean isCashier = roleNames.contains("CASHIER");

			// Seating for service & management
			if (isService || isManagement) { try { Class.forName("com.possable.seating.SeatingView"); drawer.add(new com.vaadin.flow.router.RouterLink("Seating", com.possable.seating.SeatingView.class)); } catch (ClassNotFoundException ignore) {} }
			// Menu for service, management, cashier
			if (isService || isManagement || isCashier) { try { Class.forName("com.possable.menu.MenuView"); drawer.add(new com.vaadin.flow.router.RouterLink("Menu", com.possable.menu.MenuView.class)); } catch (ClassNotFoundException ignore) {} }
			// Orders link tailored per role: prefer specific views if available
			if (isKitchen) {
				try { Class.forName("com.possable.order.kitchen.KitchenOrderView"); drawer.add(new com.vaadin.flow.router.RouterLink("Orders", com.possable.order.kitchen.KitchenOrderView.class)); } catch (ClassNotFoundException ignore) {}
			} else if (isService) {
				try { Class.forName("com.possable.order.service.ServiceOrderView"); drawer.add(new com.vaadin.flow.router.RouterLink("Orders", com.possable.order.service.ServiceOrderView.class)); } catch (ClassNotFoundException ignore) {}
			} else if (isCashier) {
				try { Class.forName("com.possable.order.cashier.CashierOrderView"); drawer.add(new com.vaadin.flow.router.RouterLink("Orders", com.possable.order.cashier.CashierOrderView.class)); } catch (ClassNotFoundException ignore) {}
			} else {
				try { Class.forName("com.possable.order.ui.OrderView"); drawer.add(new com.vaadin.flow.router.RouterLink("Orders", com.possable.order.ui.OrderView.class)); } catch (ClassNotFoundException ignore) {}
			}
			// Payment for cashier & management
			if (isCashier || isManagement) { try { Class.forName("com.possable.checkout.ui.CashierView"); drawer.add(new com.vaadin.flow.router.RouterLink("Payment", com.possable.checkout.ui.CashierView.class)); } catch (ClassNotFoundException ignore) {} }
			// Settings & Reports for management
			if (isManagement) {
				try { Class.forName("com.possable.settings.SettingsView"); drawer.add(new com.vaadin.flow.router.RouterLink("Settings", com.possable.settings.SettingsView.class)); } catch (ClassNotFoundException ignore) {}
				try {
					Class<?> reportsCls = Class.forName("com.possable.reports.ReportsView");
					@SuppressWarnings("unchecked")
					Class<? extends com.vaadin.flow.component.Component> reportsTarget = (Class<? extends com.vaadin.flow.component.Component>) reportsCls;
					drawer.add(new com.vaadin.flow.router.RouterLink("Reports", reportsTarget));
				} catch (ClassNotFoundException ignore) {}
			}
		}

		addToDrawer(drawer);

		addAttachListener(evt -> {
			// show startup message if present
			String startupMsg = this.notificationFacade.consumeStartupMessage();
			if (startupMsg != null && !startupMsg.isBlank()) {
				Notification.show(startupMsg, 5000, Notification.Position.TOP_END);
			}

			// update profile button tooltip from security context
			UI ui = evt.getUI();
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
				profile.getElement().setAttribute("title", auth.getName());
				profile.setVisible(true);
			} else {
				profile.getElement().setAttribute("title", "Not signed in");
				profile.setVisible(false);
			}

			// listen for user switch events
			broadcasterRegistration = Broadcaster.register(payload -> ui.access(() -> {
				if (payload == null || payload.isBlank()) {
					profile.getElement().setAttribute("title", "Not signed in");
					profile.setVisible(false);
					return;
				}
				// support two formats: plain username or JSON message payloads
				String trimmed = payload.trim();
				if (trimmed.startsWith("{")) {
					try {
						Map<?,?> m = new ObjectMapper().readValue(trimmed, Map.class);
						Object type = m.get("type");
						if ("access_denied".equals(String.valueOf(type))) {
							Object raw = m.get("message");
							String text = raw == null ? "Access denied" : String.valueOf(raw);
							com.vaadin.flow.component.notification.Notification.show(text, 4000, com.vaadin.flow.component.notification.Notification.Position.TOP_END);
							return;
						}
					} catch (Exception ignore) {}
				}
				// fallback: treat as username
				profile.getElement().setAttribute("title", payload);
				profile.setVisible(true);
			}));

			// register client-side listeners to report activity to server
			getElement().executeJs("const root=$0; function __vaadin_report(){ try{root.$server.reportActivity();}catch(e){} } window.addEventListener('mousemove', __vaadin_report); window.addEventListener('keydown', __vaadin_report); window.addEventListener('touchstart', __vaadin_report);", getElement());

			// initialize theme from localStorage on attach and set initial label for themeToggle
			getElement().executeJs("const key='possable-theme';let v=localStorage.getItem(key);if(!v){v=document.body.classList.contains('light-mode')?'light':'dark'}if(v==='light'){document.body.classList.add('light-mode')}else{document.body.classList.remove('light-mode')}const isLight=(v==='light');const btn=document.getElementById('theme-toggle');if(btn){btn.textContent=(isLight?'🌞':'🌙')}" );

			// start inactivity checker for this UI
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
						Broadcaster.broadcast("");
						Notification.show("Logged out due to inactivity", 3000, Notification.Position.TOP_END);
						ui.navigate(EntryPointView.class);
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
			if (broadcasterRegistration != null) {
				broadcasterRegistration.remove();
				broadcasterRegistration = null;
			}
		});

	}

	@ClientCallable
	public void reportActivity() {
		VaadinSession session = VaadinSession.getCurrent();
		if (session != null) {
			session.setAttribute("lastActivity", System.currentTimeMillis());
		}
	}

} 

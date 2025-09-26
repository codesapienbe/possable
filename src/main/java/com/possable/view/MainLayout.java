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
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainLayout extends AppLayout {

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

	public MainLayout(DemoNotificationService demoNotificationService) {
		this.demoNotificationService = demoNotificationService;

		H1 title = new H1("Possable POS");
		title.getStyle().set("margin", "0").set("font-size", "18px");

		Span status = new Span("Connected");
		status.getStyle().set("margin-left", "var(--lumo-space-m)");
		status.getElement().getThemeList().add("badge success");

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
		// theme toggle: toggles light/dark mode and persists selection in localStorage; label shows state with icon
		Button themeToggle = new Button();
		themeToggle.addClassName("pos-button-large");
		// expose id so client-side init script can target the button reliably
		themeToggle.getElement().setAttribute("id", "theme-toggle");
		// click toggles theme, persists choice and updates the button label/icon
		themeToggle.addClickListener(evt -> {
			getUI().ifPresent(ui -> ui.getPage().executeJs(
				"((btn)=>{const key='possable-theme';const cur=localStorage.getItem(key)||(document.body.classList.contains('light-mode')?'light':'dark');const next=cur==='light'?'dark':'light';if(next==='light'){document.body.classList.add('light-mode')}else{document.body.classList.remove('light-mode')}localStorage.setItem(key,next);const isLight=next==='light';btn.textContent=(isLight?'🌞 Light':'🌙 Dark');})(arguments[0])",
				themeToggle.getElement()));
		});

		HorizontalLayout header = new HorizontalLayout(title, status, user, roleBadge, themeToggle, logout);
		header.setWidthFull();
		header.setAlignItems(Alignment.CENTER);
		// add CSS class names for POS theme hooks
		header.addClassName("pos-header");
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
			// initialize theme from localStorage on attach and set initial label for themeToggle
			getElement().executeJs("const key='possable-theme';let v=localStorage.getItem(key);if(!v){v=document.body.classList.contains('light-mode')?'light':'dark'}if(v==='light'){document.body.classList.add('light-mode')}else{document.body.classList.remove('light-mode')}const isLight=(v==='light');const btn=document.getElementById('theme-toggle');if(btn){btn.textContent=(isLight?'🌞 Light':'🌙 Dark')}");

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

} 
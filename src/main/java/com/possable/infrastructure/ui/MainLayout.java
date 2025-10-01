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

import com.possable.service.DemoNotificationService;
import com.possable.service.Broadcaster;
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
	private Registration broadcasterRegistration;

	public MainLayout(DemoNotificationService demoNotificationService) {
		this.demoNotificationService = demoNotificationService;

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

		Button logout = new Button(VaadinIcon.SIGN_OUT.create(), evt -> {
			Dialog confirm = new Dialog();
			confirm.add(new Span("Are you sure you want to logout?"));
			Button yes = new Button("Logout", e -> {
				SecurityContextHolder.clearContext();
				Broadcaster.broadcast("");
				confirm.close();
				getUI().ifPresent(ui -> {
					ui.navigate(EntryPointView.class);
					ui.getPage().reload();
				});
			});
			Button no = new Button("Cancel", e -> confirm.close());
			yes.addClassName("pos-button-large");
			no.addClassName("pos-button-large");
			confirm.add(new HorizontalLayout(yes, no));
			confirm.open();
		});
		logout.getElement().setAttribute("title", "Logout");
		logout.addClassName("pos-button-icon");

		Button home = new Button(VaadinIcon.HOME.create(), e -> getUI().ifPresent(ui -> ui.navigate(EntryPointView.class)));
		home.addClassName("pos-button-large");

		Button themeToggle = new Button("🌙");
		themeToggle.addClassName("pos-button-icon");
		themeToggle.getElement().setAttribute("id", "theme-toggle");
		themeToggle.getElement().setAttribute("title", "Toggle theme");
		themeToggle.addClickListener(evt -> getUI().ifPresent(ui -> ui.getPage().executeJs(
			"((btn)=>{const key='possable-theme';const cur=localStorage.getItem(key)||(document.body.classList.contains('light-mode')?'light':'dark');const next=cur==='light'?'dark':'light';if(next==='light'){document.body.classList.add('light-mode')}else{document.body.classList.remove('light-mode')}localStorage.setItem(key,next);const isLight=next==='light';btn.textContent=(isLight?'🌞':'🌙');})(arguments[0])",
			themeToggle.getElement())));

		HorizontalLayout header = new HorizontalLayout(title, status, profile, themeToggle, home, logout);
		header.setWidthFull();
		header.setAlignItems(Alignment.CENTER);
		header.addClassName("pos-header");
		status.addClassName("pos-status");
		addClassName("pos-app");
		addToNavbar(header);

		addAttachListener(evt -> {
			// show startup message if present
			String msg = this.demoNotificationService.consumeStartupMessage();
			if (msg != null && !msg.isBlank()) {
				Notification.show(msg, 5000, Notification.Position.TOP_END);
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
			broadcasterRegistration = Broadcaster.register(name -> ui.access(() -> {
				if (name == null || name.isBlank()) {
					profile.getElement().setAttribute("title", "Not signed in");
					profile.setVisible(false);
				} else {
					profile.getElement().setAttribute("title", name);
					profile.setVisible(true);
				}
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

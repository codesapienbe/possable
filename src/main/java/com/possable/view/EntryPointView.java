package com.possable.view;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.possable.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Entry")
public class EntryPointView extends VerticalLayout {

	private final UserService userService;
	private final Div unlockOverlay;
	private final Div unlockLock;

	@Autowired
	public EntryPointView(UserService userService) {
		this.userService = userService;
		this.unlockOverlay = new Div();
		this.unlockLock = new Div();
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		// make the entry view fill the viewport and center its contents (header + role cards)
		setHeightFull();
		setAlignItems(FlexComponent.Alignment.CENTER);
		setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		addClassName("pos-root");
		addClassName("entry-screen");

		H1 header = new H1("Welcome");
		header.getStyle().set("font-size", "1.8em");
		add(header);

		// role selection cards (icons, centered)
		HorizontalLayout cards = new HorizontalLayout();
		cards.addClassName("role-cards");
		cards.setWidthFull();
		cards.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		cards.getStyle().set("gap", "18px");
		cards.addClassName("role-cards");

		cards.add(createRoleCard(VaadinIcon.USERS, "SERVICE", () -> openPinDialog("service")));
		cards.add(createRoleCard(VaadinIcon.CUTLERY, "KITCHEN", () -> openPinDialog("kitchen")));
			cards.add(createRoleCard(VaadinIcon.COG, "MANAGEMENT", () -> openPinDialog("management")));
			cards.add(createRoleCard(VaadinIcon.SHOP, "CUSTOMER", () -> UI.getCurrent().navigate("customer")));
			// future: capture drawing-based unlock input via client and pass to openPinDialog as drawing parameter

		add(cards);

		// unlock overlay setup (hidden by default)
		unlockOverlay.addClassName("unlock-overlay");
		unlockLock.addClassName("unlock-lock");
		unlockLock.setText("UNLOCKING");
		unlockOverlay.add(unlockLock);
		unlockOverlay.setVisible(false);
		add(unlockOverlay);
	}

	private Button createRoleCard(VaadinIcon icon, String text, Runnable action) {
		VerticalLayout inner = new VerticalLayout();
		inner.setPadding(false);
		inner.setSpacing(false);
		inner.setAlignItems(FlexComponent.Alignment.CENTER);
		Icon ico = icon.create();
		ico.getStyle().set("font-size", "36px");
		Span label = new Span(text);
		label.getStyle().set("margin-top", "8px").set("font-weight", "700");
		inner.add(ico, label);
		// use Button(Component...) constructor to compose content (avoids protected add)
		Button card = new Button(inner);
		card.addClassName("pos-card");
		card.addClassName("pos-button-large");
		// add role-specific classname so we can theme icon color/accents via CSS
		card.addClassName("role-" + text.toLowerCase());
		card.getStyle().set("min-width", "160px");
		card.addClickListener(e -> action.run());
		return card;
	}

	private void openPinDialog(String username) {
		Dialog dialog = new Dialog();
		dialog.setWidth("420px");
		// keep dialog height constrained on small viewports to avoid full-page scroll
		dialog.getElement().getStyle().set("max-height", "80vh");
		// add a helper classname for targeted styling
		dialog.addClassName("pin-dialog");
		VerticalLayout content = new VerticalLayout();
		content.setPadding(true);
		content.setSpacing(true);
		content.setAlignItems(FlexComponent.Alignment.CENTER);
		content.add(new H1("Enter PIN"));
		TextField pin = new TextField("PIN");
		pin.setPlaceholder("4-digit PIN");
		// use a fixed width so the numeric keypad and dots align nicely
		pin.setWidth("220px");
		pin.getElement().setAttribute("inputmode", "numeric");
		pin.getElement().setAttribute("maxlength", "4");
		pin.setValue("");
		pin.setReadOnly(true); // force using on-screen keypad

		// visual PIN indicator (four dots)
		HorizontalLayout pinDots = new HorizontalLayout();
		pinDots.getStyle().set("gap", "8px");
		java.util.List<com.vaadin.flow.component.html.Span> dotSpans = new java.util.ArrayList<>();
		for (int i = 0; i < 4; i++) {
			com.vaadin.flow.component.html.Span s = new com.vaadin.flow.component.html.Span();
			s.addClassName("pin-dot");
			dotSpans.add(s);
			pinDots.add(s);
		}

		java.lang.Runnable refreshDots = () -> {
			String v = pin.getValue() == null ? "" : pin.getValue();
			for (int i = 0; i < dotSpans.size(); i++) {
				if (i < v.length()) {
					dotSpans.get(i).getElement().getClassList().add("filled");
				} else {
					dotSpans.get(i).getElement().getClassList().remove("filled");
				}
			}
		};
		// pattern lock component (small square)
		// choose hover color by role to provide visual context
		String hoverColor = "rgba(255,255,255,0.6)";
		switch (username) {
			case "kitchen" -> hoverColor = "rgba(245,158,11,0.9)";
			case "management" -> hoverColor = "rgba(6,182,212,0.9)";
			case "service" -> hoverColor = "rgba(14,165,164,0.9)";
			default -> hoverColor = "rgba(167,139,250,0.9)";
		}
		PatternLockComponent pattern = new PatternLockComponent(260, hoverColor, 14);
		content.add(pattern);
		// declare numpad early so the toggle can control its visibility
		HorizontalLayout numpad = new HorizontalLayout();
		numpad.addClassName("pin-numpad");
		numpad.setVisible(false);

		// hide PIN elements by default — drawing is primary login method
		pin.setVisible(false);
		pinDots.setVisible(false);

		// toggle switch between pattern (default) and PIN
		Button usePinToggle = new Button();
		usePinToggle.addClassName("toggle-switch");
		usePinToggle.getElement().setProperty("aria-pressed", "false");
		// initial state: off (pattern visible)
		usePinToggle.getElement().getClassList().remove("on");
		usePinToggle.addClickListener(e -> {
			boolean isOn = usePinToggle.getElement().getClassList().contains("on");
			if (isOn) {
				// switch to pattern
				usePinToggle.getElement().getClassList().remove("on");
				usePinToggle.getElement().setProperty("aria-pressed", "false");
				pattern.setVisible(true);
				pin.setVisible(false);
				pinDots.setVisible(false);
				numpad.setVisible(false);
			} else {
				// switch to PIN input
				usePinToggle.getElement().getClassList().add("on");
				usePinToggle.getElement().setProperty("aria-pressed", "true");
				pattern.setVisible(false);
				pin.setVisible(true);
				pinDots.setVisible(true);
				numpad.setVisible(true);
			}
		});
		// visible label for the toggle (helps when button visuals blend with background)
		Span usePinLabel = new Span("Use PIN");
		usePinLabel.addClassName("toggle-label");
		// wrapper to align toggle and label
		HorizontalLayout toggleRow = new HorizontalLayout(usePinToggle, usePinLabel);
		toggleRow.setAlignItems(FlexComponent.Alignment.CENTER);
		toggleRow.addClassName("toggle-row");
		content.add(toggleRow);

		// listen for pattern changes from client component and attempt login when pattern length >= 4
		pattern.getElement().addEventListener("pattern-changed", ev -> {
			String patternJson = ev.getEventData().getString("event.detail.pattern");
			if (patternJson == null || patternJson.isBlank()) return;
			// count indices in the JSON array
			String cleaned = patternJson.replaceAll("[^0-9,]", "");
			String[] parts = cleaned.isEmpty() ? new String[0] : cleaned.split(",");
			if (parts.length < 4) return; // require at least 4 nodes to consider a pattern
			boolean ok = userService.authenticate(username, patternJson, null);
			if (ok) {
				completeLogin(username, dialog, pattern);
			} else {
				Notification.show("Invalid pattern");
				pattern.clear();
			}
		}).addEventData("event.detail.pattern");

		Button submit = new Button("Enter", evt -> {
			String value = pin.getValue();
			try {
				if (value == null || !value.matches("\\d{4,6}")) {
					Notification.show("Please enter a 4-6 digit PIN");
					return;
				}
				// first try drawing-based auth using pattern lock
				String drawing = pattern.getPattern();
				boolean ok = userService.authenticate(username, drawing != null && !drawing.isBlank() ? drawing : null, value);
				if (ok) {
					completeLogin(username, dialog, pattern);
				} else {
					Notification.show("Invalid PIN");
				}
			} catch (Exception ex) {
				Notification.show("Invalid PIN");
			}
		});
		Button cancel = new Button("Cancel", evt -> dialog.close());
		submit.addClassName("pos-button-large");
		cancel.addClassName("pos-button-large");
		// numeric keypad
		numpad.getStyle().set("flex-wrap", "wrap").set("gap", "8px").set("max-width", "260px");
		// center keypad buttons and make it addressable from CSS
		numpad.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		for (int i = 1; i <= 9; i++) {
			int digit = i;
			Button d = new Button(Integer.toString(digit), e -> {
				String v = pin.getValue();
				if (v.length() < 4) {
					pin.setValue(v + digit);
					refreshDots.run();
					if (pin.getValue().length() == 4) submit.click();
				}
			});
			d.addClassName("pos-button-large");
			d.addClassName("pin-key");
			d.getStyle().set("width", "64px");
			numpad.add(d);
		}
		Button zero = new Button("0", e -> { String v = pin.getValue(); if (v.length() < 4) { pin.setValue(v + "0"); refreshDots.run(); if (pin.getValue().length() == 4) submit.click(); } });
		zero.addClassName("pos-button-large"); zero.addClassName("pin-key"); zero.getStyle().set("width", "64px");
		Button back = new Button("⌫", e -> { String v = pin.getValue(); if (!v.isEmpty()) { pin.setValue(v.substring(0, v.length() - 1)); refreshDots.run(); } });
		back.addClassName("pos-button-large"); back.addClassName("pin-key"); back.getStyle().set("width", "64px");
		Button clearBtn = new Button("Clear", e -> { pin.setValue(""); refreshDots.run(); });
		clearBtn.addClassName("pos-button-large"); clearBtn.addClassName("pin-key"); clearBtn.getStyle().set("width", "64px");
		numpad.add(zero, back, clearBtn);
		// add numpad to content (it may be hidden initially)
		content.add(pinDots, pin, numpad, new HorizontalLayout(submit, cancel));
		dialog.add(content);
		dialog.open();
	}

	private void completeLogin(String username, Dialog dialog, PatternLockComponent pattern) {
		// prepare role-specific unlock display
		unlockLock.getElement().getClassList().remove("role-service");
		unlockLock.getElement().getClassList().remove("role-kitchen");
		unlockLock.getElement().getClassList().remove("role-management");
		unlockLock.getElement().getClassList().remove("role-customer");
		unlockLock.removeAll();
		Icon roleIcon;
		Span roleLabel = new Span(username.toUpperCase());
		switch (username) {
		case "kitchen" -> {
			unlockLock.getElement().getClassList().add("role-kitchen");
			roleIcon = VaadinIcon.CUTLERY.create();
		}
		case "management" -> {
			unlockLock.getElement().getClassList().add("role-management");
			roleIcon = VaadinIcon.COG.create();
		}
		case "service" -> {
			unlockLock.getElement().getClassList().add("role-service");
			roleIcon = VaadinIcon.USERS.create();
		}
		default -> {
			unlockLock.getElement().getClassList().add("role-customer");
			roleIcon = VaadinIcon.SHOP.create();
		}
		}
		roleIcon.getStyle().set("font-size", "42px");
		roleLabel.getStyle().set("margin-top", "8px").set("font-weight", "800");
		unlockLock.add(roleIcon, roleLabel);

		// set security context with username and mapped roles
		var roles = userService.getRoles(username).stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList());
		var token = new UsernamePasswordAuthenticationToken(username, null, roles);
		SecurityContextHolder.getContext().setAuthentication(token);
		dialog.close();
		Notification.show("Welcome " + username);
		// show unlock animation overlay and delay navigation until finished
		unlockOverlay.setVisible(true);
		UI ui = UI.getCurrent();
		Thread animThread = new Thread(() -> {
			try { Thread.sleep(900); } catch (InterruptedException ignored) {}
			ui.access(() -> {
				unlockOverlay.setVisible(false);
				String targetRoute = switch (username) {
					case "kitchen" -> "kitchen";
					case "management" -> "management";
					case "service" -> "service";
					default -> "customer";
				};
				ui.navigate(targetRoute);
			});
		});
		animThread.setDaemon(true);
		animThread.start();
		// clear pattern after use
		if (pattern != null) pattern.clear();
	}
} 
package com.possable.view;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

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
		addClassName("pos-root");

		H1 header = new H1("Welcome");
		header.getStyle().set("font-size", "1.8em");
		add(header);

		// role selection cards (icons, centered)
		HorizontalLayout cards = new HorizontalLayout();
		cards.setWidthFull();
		cards.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		cards.getStyle().set("gap", "18px");

		cards.add(createRoleCard(VaadinIcon.USERS, "SERVICE", () -> openPinDialog("service")));
		cards.add(createRoleCard(VaadinIcon.CUTLERY, "KITCHEN", () -> openPinDialog("kitchen")));
		cards.add(createRoleCard(VaadinIcon.COG, "MANAGEMENT", () -> openPinDialog("management")));
		cards.add(createRoleCard(VaadinIcon.SHOP, "CUSTOMER", () -> UI.getCurrent().navigate("customer")));

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
		card.setWidth("160px");
		card.addClickListener(e -> action.run());
		return card;
	}

	private void openPinDialog(String username) {
		Dialog dialog = new Dialog();
		dialog.setWidth("320px");
		VerticalLayout content = new VerticalLayout();
		content.setPadding(true);
		content.setSpacing(true);
		content.add(new H1("Enter PIN"));
		TextField pin = new TextField("PIN");
		pin.setPlaceholder("4-digit PIN");
		pin.setWidthFull();
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
		Button submit = new Button("Enter", evt -> {
			String value = pin.getValue();
			try {
				if (value == null || !value.matches("\\d{4}")) {
					Notification.show("Please enter a 4-digit PIN");
					return;
				}
				boolean ok = userService.authenticate(username, value);
				if (ok) {
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
						try {
							Thread.sleep(900); // duration matches CSS animation
						} catch (InterruptedException ignored) {}
						ui.access(() -> {
						unlockOverlay.setVisible(false);
						// navigate depending on role/user (go to role landing pages by route)
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
		HorizontalLayout numpad = new HorizontalLayout();
		numpad.getStyle().set("flex-wrap", "wrap").set("gap", "8px").set("max-width", "260px");
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
			d.getStyle().set("width", "64px");
			numpad.add(d);
		}
		Button zero = new Button("0", e -> { String v = pin.getValue(); if (v.length() < 4) { pin.setValue(v + "0"); refreshDots.run(); if (pin.getValue().length() == 4) submit.click(); } });
		zero.addClassName("pos-button-large"); zero.getStyle().set("width", "64px");
		Button back = new Button("⌫", e -> { String v = pin.getValue(); if (!v.isEmpty()) { pin.setValue(v.substring(0, v.length() - 1)); refreshDots.run(); } });
		back.addClassName("pos-button-large"); back.getStyle().set("width", "64px");
		Button clearBtn = new Button("Clear", e -> { pin.setValue(""); refreshDots.run(); });
		clearBtn.addClassName("pos-button-large"); clearBtn.getStyle().set("width", "64px");
		numpad.add(zero, back, clearBtn);
		content.add(pinDots, pin, numpad, new HorizontalLayout(submit, cancel));
		dialog.add(content);
		dialog.open();
	}
} 
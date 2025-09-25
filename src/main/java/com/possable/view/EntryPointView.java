package com.possable.view;

import com.possable.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
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

	@Autowired
	public EntryPointView(UserService userService) {
		this.userService = userService;
		this.unlockOverlay = new Div();
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		H1 header = new H1("Welcome");
		header.getStyle().set("font-size", "1.8em");
		add(header);

		HorizontalLayout actions = new HorizontalLayout();
		actions.setWidthFull();
		Button service = new Button("SERVICE", evt -> openPinDialog("service"));
		service.addClassName("pos-button-large");
		Button kitchen = new Button("KITCHEN", evt -> openPinDialog("kitchen"));
		kitchen.addClassName("pos-button-large");
		Button management = new Button("MANAGEMENT", evt -> openPinDialog("management"));
		management.addClassName("pos-button-large");
		Button customer = new Button("CUSTOMER", evt -> {
			UI.getCurrent().navigate(ItemListView.class);
		});
		customer.addClassName("pos-button-large");

		actions.add(service, kitchen, management, customer);
		add(actions);

		// unlock overlay setup (hidden by default)
		unlockOverlay.addClassName("unlock-overlay");
		Div unlockLock = new Div();
		unlockLock.addClassName("unlock-lock");
		unlockLock.setText("UNLOCKING");
		unlockOverlay.add(unlockLock);
		unlockOverlay.setVisible(false);
		add(unlockOverlay);
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
		Button submit = new Button("Enter", evt -> {
			String value = pin.getValue();
			try {
				if (value == null || !value.matches("\\d{4}")) {
					Notification.show("Please enter a 4-digit PIN");
					return;
				}
				boolean ok = userService.authenticate(username, value);
				if (ok) {
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
							Thread.sleep(800); // duration matches CSS animation
						} catch (InterruptedException ignored) {}
						ui.access(() -> {
							unlockOverlay.setVisible(false);
							// navigate depending on role/user
							switch (username) {
							case "kitchen" -> ui.navigate(OrderView.class);
							case "management" -> ui.navigate(PrinterListView.class);
							default -> ui.navigate(DashboardView.class);
							}
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
				if (v.length() < 4) pin.setValue(v + digit);
			});
			d.addClassName("pos-button-large");
			d.getStyle().set("width", "64px");
			numpad.add(d);
		}
		Button zero = new Button("0", e -> { String v = pin.getValue(); if (v.length() < 4) pin.setValue(v + "0"); });
		zero.addClassName("pos-button-large"); zero.getStyle().set("width", "64px");
		Button back = new Button("⌫", e -> { String v = pin.getValue(); if (!v.isEmpty()) pin.setValue(v.substring(0, v.length() - 1)); });
		back.addClassName("pos-button-large"); back.getStyle().set("width", "64px");
		Button clearBtn = new Button("Clear", e -> pin.setValue(""));
		clearBtn.addClassName("pos-button-large"); clearBtn.getStyle().set("width", "64px");
		numpad.add(zero, back, clearBtn);
		content.add(pin, numpad, new HorizontalLayout(submit, cancel));
		dialog.add(content);
		dialog.open();
	}
} 
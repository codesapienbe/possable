package com.possable.view;

import com.possable.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
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

	@Autowired
	public EntryPointView(UserService userService) {
		this.userService = userService;
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
	}

	private void openPinDialog(String username) {
		Dialog dialog = new Dialog();
		dialog.setWidth("320px");
		VerticalLayout content = new VerticalLayout();
		content.setPadding(true);
		content.setSpacing(true);
		content.add(new H1("Enter PIN"));
		PasswordField pin = new PasswordField("PIN");
		pin.setPlaceholder("4-digit PIN");
		pin.setWidthFull();
		Button submit = new Button("Enter", evt -> {
			String value = pin.getValue();
			try {
				boolean ok = userService.authenticate(username, value);
				if (ok) {
					// set security context with username and mapped roles
					var roles = userService.getRoles(username).stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList());
					var token = new UsernamePasswordAuthenticationToken(username, null, roles);
					SecurityContextHolder.getContext().setAuthentication(token);
					dialog.close();
					Notification.show("Welcome " + username);
					// navigate depending on role/user
					switch (username) {
					case "kitchen" -> UI.getCurrent().navigate(OrderView.class);
					case "management" -> UI.getCurrent().navigate(PrinterListView.class);
					default -> UI.getCurrent().navigate(DashboardView.class);
					}
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
		content.add(pin, new HorizontalLayout(submit, cancel));
		dialog.add(content);
		dialog.open();
	}
} 
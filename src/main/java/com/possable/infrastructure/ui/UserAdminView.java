package com.possable.infrastructure.ui;

import java.util.Set;

import com.possable.user.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "admin/users", layout = com.possable.user.UserMainLayout.class)
@PageTitle("User Admin")
public class UserAdminView extends VerticalLayout implements BeforeEnterObserver {

	private final UserService userService;
	private final Grid<String> grid = new Grid<>();

	public UserAdminView(UserService userService) {
		this.userService = userService;
		setPadding(true);
		setSpacing(true);
		addClassName("pos-root");

		add(new H1("User Management"));

		grid.addColumn(s -> s).setHeader("Username");
		refreshGrid();
		add(grid);

		TextField username = new TextField("Username");
		PasswordField pin = new PasswordField("PIN");
		TextField drawing = new TextField("Drawing (line data)");
		Button add = new Button("Add User", evt -> {
			if (username.isEmpty() || pin.isEmpty()) { Notification.show("Username and PIN required"); return; }
			boolean ok = userService.addUser(username.getValue(), pin.getValue(), drawing.getValue(), Set.of("SERVICE"));
			if (ok) { Notification.show("User added"); refreshGrid(); username.clear(); pin.clear(); } else { Notification.show("Failed"); }
		});
		add.addClassName("pos-button-large");

		Button remove = new Button("Remove Selected", evt -> {
			var sel = grid.asSingleSelect().getValue();
			if (sel == null) { Notification.show("Select a user"); return; }
			if (userService.removeUser(sel)) { Notification.show("Removed"); refreshGrid(); } else { Notification.show("Failed"); }
		});
		remove.addClassName("pos-button-large");

		Button change = new Button("Change PIN", evt -> {
			var sel = grid.asSingleSelect().getValue();
			if (sel == null) { Notification.show("Select a user"); return; }
			if (pin.isEmpty()) { Notification.show("Enter new PIN"); return; }
			if (userService.updatePincode(sel, pin.getValue())) { Notification.show("Updated"); refreshGrid(); pin.clear(); } else { Notification.show("Failed"); }
		});
		change.addClassName("pos-button-large");

		HorizontalLayout form = new HorizontalLayout(username, pin, add, remove, change);
		form.setWidthFull();
		add(form);
	}

	private void refreshGrid() {
		Set<String> users = userService.listUsernames();
		grid.setItems(users);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean allowed = false;
		if (auth != null && auth.isAuthenticated()) {
			allowed = auth.getAuthorities().stream().anyMatch(a -> "ROLE_MANAGEMENT".equals(a.getAuthority()));
		}
		if (!allowed) {
			Notification.show("Access denied: management only", 3000, Notification.Position.TOP_END);
			event.rerouteTo(AccessDeniedView.class);
		}
	}
} 

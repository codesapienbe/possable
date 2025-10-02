package com.possable.user.ui;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.possable.user.UserService;
import com.possable.infrastructure.Broadcaster;
import com.possable.user.UserMainLayout;
import com.possable.infrastructure.ui.EntryPointView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.dialog.Dialog;

@Route(value = "profile", layout = UserMainLayout.class)
@PageTitle("Profile")
public class ProfileView extends VerticalLayout {

    private final UserService userService;
    private TextField displayName;
    private TextField email;
    private TextField phone;
    private Image avatarImg;
    private String username;

    private Div apiKeysContainer;

    @Autowired
    public ProfileView(UserService userService) {
        this.userService = userService;
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Profile");
        add(title);

        // Top area: avatar + profile form (two-column)
        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setAlignItems(FlexComponent.Alignment.START);
        top.getStyle().set("gap", "28px");

        // Avatar column
        VerticalLayout avatarCol = new VerticalLayout();
        avatarCol.setPadding(false);
        avatarCol.setSpacing(true);
        avatarCol.setWidth("220px");

        avatarImg = new Image();
        avatarImg.setWidth("140px");
        avatarImg.setHeight("140px");
        avatarImg.getStyle().set("border-radius", "8px").set("background", "var(--pos-card)");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/*");
        upload.setMaxFiles(1);
        upload.getElement().getStyle().set("margin-top", "8px");
        upload.addSucceededListener(evt -> {
            try {
                var in = buffer.getInputStream();
                byte[] bytes = in.readAllBytes();
                String contentType = Objects.toString(evt.getMIMEType(), "image/png");
                boolean ok = userService.uploadAvatar(username, bytes, contentType);
                if (ok) {
                    loadAvatar();
                    Broadcaster.broadcast(username);
                    Notification.show("Avatar uploaded", 2000, Notification.Position.TOP_CENTER);
                } else {
                    Notification.show("Failed to save avatar", 3000, Notification.Position.TOP_CENTER);
                }
            } catch (Exception ex) {
                Notification.show("Failed to upload avatar", 3000, Notification.Position.TOP_CENTER);
            }
        });

        avatarCol.add(avatarImg, upload);

        // Form column
        VerticalLayout formCol = new VerticalLayout();
        formCol.setPadding(false);
        formCol.setSpacing(true);
        formCol.setWidthFull();

        displayName = new TextField("Display name");
        displayName.setWidth("360px");
        email = new TextField("Email");
        email.setWidth("360px");
        phone = new TextField("Phone");
        phone.setWidth("360px");

        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        Button save = new Button("Save", e -> saveProfile());
        save.addClassName("pos-button-large");
        Button changePin = new Button("Change PIN", e -> openChangePin());
        changePin.addClassName("pos-button-large");
        actions.add(save, changePin);

        // roles display
        HorizontalLayout rolesRow = new HorizontalLayout();
        rolesRow.setSpacing(true);

        formCol.add(displayName, email, phone, rolesRow, actions);

        top.add(avatarCol, formCol);
        add(top);

        // API Keys section (clean placement)
        apiKeysContainer = new Div();
        apiKeysContainer.getStyle().set("margin-top", "18px");
        add(new H2("API Keys"));
        add(apiKeysContainer);

		// Logout button moved here from header
		Button logoutBtn = new Button("Logout", evt -> {
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
		logoutBtn.addClassName("pos-button-large");
		add(logoutBtn);

        addAttachListener(evt -> loadProfileAndRender());
    }

    private void loadProfileAndRender() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            getUI().ifPresent(ui -> ui.navigate(EntryPointView.class));
            return;
        }
        username = a.getName();
        var map = userService.getProfile(username);
        displayName.setValue((String)map.getOrDefault("displayName", username));
        email.setValue((String)map.getOrDefault("email", ""));
        phone.setValue((String)map.getOrDefault("phone", ""));
        // roles
        var roles = userService.getRoles(username);
        renderRoles(roles);
        loadAvatar();
        renderApiKeys();
    }

    private void renderRoles(java.util.Set<String> roles) {
        VerticalLayout parent = (VerticalLayout) displayName.getParent().get();
        // find existing rolesRow (4th component)
        HorizontalLayout rolesRow = (HorizontalLayout) parent.getComponentAt(3);
        rolesRow.removeAll();
        if (roles == null || roles.isEmpty()) return;
        for (String r : roles) {
            Span badge = new Span(r);
            badge.addClassName("pos-role-badge");
            // role-specific classes
            if ("SERVICE".equalsIgnoreCase(r)) badge.addClassName("role-service");
            if ("KITCHEN".equalsIgnoreCase(r)) badge.addClassName("role-kitchen");
            if ("MANAGEMENT".equalsIgnoreCase(r)) badge.addClassName("role-management");
            rolesRow.add(badge);
        }
    }

    private void renderApiKeys() {
        apiKeysContainer.removeAll();
        var keys = userService.listApiKeys(username);
        if (keys == null || keys.isEmpty()) {
            Paragraph info = new Paragraph("No API keys created. Use Create API Key to generate a token.");
            Button create = new Button("Create API Key", e -> createKey());
            create.addClassName("pos-button-large");
            apiKeysContainer.add(info, create);
            return;
        }
        Grid<Map.Entry<String,String>> grid = new Grid<>();
        grid.addColumn(Map.Entry::getKey).setHeader("Key ID");
        grid.addColumn(Map.Entry::getValue).setHeader("Masked");
        grid.addComponentColumn(entry -> {
            Button revoke = new Button(VaadinIcon.TRASH.create());
            revoke.addClickListener(e -> { revokeKey(entry.getKey()); renderApiKeys(); });
            return revoke;
        }).setHeader("Actions");
        grid.setItems(keys.entrySet());
        grid.setWidth("720px");
        Button create = new Button("Create API Key", e -> { createKey(); renderApiKeys(); });
        create.addClassName("pos-button-large");
        apiKeysContainer.add(grid, create);
    }

    private void loadAvatar() {
        var av = userService.getAvatar(username);
        if (av == null || av.isEmpty()) {
            avatarImg.setSrc(ThemeUtils.placeholderAvatar());
            return;
        }
        byte[] bytes = (byte[]) av.get("bytes");
        String ct = (String) av.get("contentType");
        StreamResource res = new StreamResource(username + "-avatar", () -> new ByteArrayInputStream(bytes));
        res.setContentType(ct);
        avatarImg.setSrc(res);
    }

    private void saveProfile() {
        boolean ok = userService.updateProfile(username, displayName.getValue(), email.getValue(), phone.getValue());
        if (ok) {
            Broadcaster.broadcast(username);
            Notification.show("Profile saved", 2000, Notification.Position.TOP_CENTER);
        } else {
            Notification.show("Failed to save profile", 3000, Notification.Position.TOP_CENTER);
        }
    }

    private void openChangePin() {
        ChangePinDialog d = new ChangePinDialog(username, userService);
        d.open();
    }

    private void createKey() {
        String token = userService.createApiKey(username);
        if (token == null) {
            Notification.show("Failed to create API key", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        var dlg = new com.vaadin.flow.component.dialog.Dialog();
        dlg.add(new com.vaadin.flow.component.html.Paragraph("API Key (store it now, it will not be shown again)"));
        com.vaadin.flow.component.textfield.TextArea t = new com.vaadin.flow.component.textfield.TextArea();
        t.setValue(token);
        t.setWidthFull();
        t.setReadOnly(true);
        Button ok = new Button("Close", e -> dlg.close());
        dlg.add(t, ok);
        dlg.open();
    }

    private void revokeKey(String keyId) {
        boolean ok = userService.revokeApiKey(username, keyId);
        if (ok) {
            Notification.show("Key revoked", 2000, Notification.Position.TOP_CENTER);
        } else {
            Notification.show("Failed to revoke", 3000, Notification.Position.TOP_CENTER);
        }
    }
}

// small dialog class for PIN change
class ChangePinDialog extends com.vaadin.flow.component.dialog.Dialog {
    public ChangePinDialog(String username, UserService userService) {
        setWidth("420px");
        var layout = new VerticalLayout();
        var cur = new TextField("Current PIN");
        var nw = new TextField("New PIN (4-6 digits)");
        var confirm = new TextField("Confirm new PIN");
        Button submit = new Button("Change", e -> {
            String c = cur.getValue();
            String n = nw.getValue();
            String cnf = confirm.getValue();
            if (c == null || n == null || cnf == null || !n.equals(cnf) || !n.matches("\\d{4,6}")) {
                Notification.show("Invalid input");
                return;
            }
            boolean auth = userService.authenticate(username, null, c);
            if (!auth) { Notification.show("Current PIN incorrect"); return; }
            boolean ok = userService.updatePincode(username, n);
            if (ok) { Notification.show("PIN updated"); close(); } else { Notification.show("Failed to update PIN"); }
        });
        Button cancel = new Button("Cancel", e -> close());
        layout.add(cur, nw, confirm, new HorizontalLayout(submit, cancel));
        add(layout);
    }
}

// ThemeUtils moved to com.possable.user.ui.ThemeUtils 

package com.possable.infrastructure.ui;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.possable.user.UserFacade;
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

@Route(value = "", layout = com.possable.infrastructure.ui.MainLayout.class)
@PageTitle("Entry")
public class EntryPointView extends VerticalLayout {

	private final UserFacade userFacade;
	private final Div unlockOverlay;
	private final Div unlockLock;

	@Autowired
	public EntryPointView(UserFacade userFacade) {
		this.userFacade = userFacade;
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
		cards.add(createRoleCard(VaadinIcon.CREDIT_CARD, "CASHIER", () -> openPinDialog("cashier")));
		// future: capture drawing-based unlock input via client and pass to openPinDialog as drawing parameter

		add(cards);

		// place Customer button separately since it doesn't depend on other modules
		HorizontalLayout customerRow = new HorizontalLayout();
		customerRow.setWidthFull();
		customerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		customerRow.add(createRoleCard(VaadinIcon.SHOP, "CUSTOMER", () -> openCustomerPincodeDialog()));
		add(customerRow);

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
		dialog.getElement().getStyle().set("max-height", "80vh");
		dialog.addClassName("pin-dialog");

		VerticalLayout content = new VerticalLayout();
		content.setPadding(true);
		content.setSpacing(true);
		content.setAlignItems(FlexComponent.Alignment.CENTER);
		content.add(new H1("Enter PIN"));

		TextField pin = new TextField("PIN");
		pin.setPlaceholder("4-digit PIN");
		pin.getElement().setAttribute("inputmode", "numeric");
		pin.getElement().setAttribute("maxlength", "6");
		pin.setWidth("220px");
		pin.setReadOnly(true);

		HorizontalLayout pinDots = new HorizontalLayout();
		pinDots.getStyle().set("gap", "8px");
		List<com.vaadin.flow.component.html.Span> dotSpans = new java.util.ArrayList<>();
		for (int i = 0; i < 4; i++) {
			com.vaadin.flow.component.html.Span s = new com.vaadin.flow.component.html.Span();
			s.addClassName("pin-dot");
			dotSpans.add(s);
			pinDots.add(s);
		}

		Runnable refreshDots = () -> {
			String v = pin.getValue() == null ? "" : pin.getValue();
			for (int i = 0; i < dotSpans.size(); i++) {
				if (i < v.length()) dotSpans.get(i).getElement().getClassList().add("filled"); else dotSpans.get(i).getElement().getClassList().remove("filled");
			}
		};

		Button submit = new Button("Enter", evt -> {
			String value = pin.getValue();
			try {
				if (value == null || !value.matches("\\d{4,6}")) { Notification.show("Please enter a 4-6 digit PIN"); return; }
				boolean ok = userFacade.authenticate(username, null, value);
				if (ok) {
					completeLogin(username, dialog);
				} else {
					Notification.show("Invalid PIN");
				}
			} catch (Exception ex) { Notification.show("Invalid PIN"); }
		});

		Button cancel = new Button("Cancel", evt -> dialog.close());
		submit.addClassName("pos-button-large"); cancel.addClassName("pos-button-large");

		HorizontalLayout numpad = new HorizontalLayout();
		numpad.addClassName("pin-numpad");
		numpad.getStyle().set("flex-wrap", "wrap").set("gap", "8px").set("max-width", "260px");
		numpad.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

		for (int i = 1; i <= 9; i++) {
			int digit = i;
			Button d = new Button(Integer.toString(digit));
			d.addClickListener(e -> {
				String v = pin.getValue();
				if (v.length() < 6) { pin.setValue(v + digit); refreshDots.run(); if (pin.getValue().length() >= 4) submit.focus(); }
				d.addClassName("press-anim");
				// remove animation class after animation duration
				new Thread(() -> { try { Thread.sleep(160); UI.getCurrent().access(() -> d.removeClassName("press-anim")); } catch (Exception ignored) {} }).start();
			});
			d.addClassName("pos-button-large"); d.addClassName("pin-key"); d.getStyle().set("width", "64px"); numpad.add(d);
		}
		Button zero = new Button("0");
		zero.addClickListener(e -> {
			String v = pin.getValue(); if (v.length() < 6) { pin.setValue(v + "0"); refreshDots.run(); if (pin.getValue().length() >= 4) submit.focus(); }
			zero.addClassName("press-anim"); new Thread(() -> { try { Thread.sleep(160); UI.getCurrent().access(() -> zero.removeClassName("press-anim")); } catch (Exception ignored) {} }).start();
		});
		zero.addClassName("pos-button-large"); zero.addClassName("pin-key"); zero.getStyle().set("width", "64px");
		Button back = new Button("⌫");
		back.addClickListener(e -> { String v = pin.getValue(); if (!v.isEmpty()) { pin.setValue(v.substring(0, v.length() - 1)); refreshDots.run(); } back.addClassName("press-anim"); new Thread(() -> { try { Thread.sleep(160); UI.getCurrent().access(() -> back.removeClassName("press-anim")); } catch (Exception ignored) {} }).start(); });
		back.addClassName("pos-button-large"); back.addClassName("pin-key"); back.getStyle().set("width", "64px");
		Button clearBtn = new Button("Clear");
		clearBtn.addClickListener(e -> { pin.setValue(""); refreshDots.run(); clearBtn.addClassName("press-anim"); new Thread(() -> { try { Thread.sleep(160); UI.getCurrent().access(() -> clearBtn.removeClassName("press-anim")); } catch (Exception ignored) {} }).start(); });
		clearBtn.addClassName("pos-button-large"); clearBtn.addClassName("pin-key"); clearBtn.getStyle().set("width", "64px");
		numpad.add(zero, back, clearBtn);

		content.add(pinDots, pin, numpad, new HorizontalLayout(submit, cancel));
		dialog.add(content);
		dialog.open();
	}

	private void completeLogin(String username, Dialog dialog) {
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
		var roles = userFacade.getRoles(username).stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList());
		var token = new UsernamePasswordAuthenticationToken(username, null, roles);
		SecurityContextHolder.getContext().setAuthentication(token);
		dialog.close();
		// notify layout(s) that user changed so profile and header can update
		com.possable.infrastructure.Broadcaster.broadcast(username);
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
		// no pattern component used any more
	}

	private void openCustomerPincodeDialog() {
		Dialog dialog = new Dialog();
		dialog.setWidth("420px");
		VerticalLayout content = new VerticalLayout();
		content.setPadding(true);
		content.setSpacing(true);
		content.setAlignItems(FlexComponent.Alignment.CENTER);
		content.add(new H1("Customer - Enter PIN"));
		TextField table = new TextField("Table (optional)");
		table.setWidth("220px");
		TextField pin = new TextField("PIN");
		pin.setPlaceholder("4-digit PIN");
		pin.getElement().setAttribute("inputmode", "numeric");
		pin.getElement().setAttribute("maxlength", "6");
		pin.setWidth("220px");

		Button submit = new Button("Enter", evt -> {
			String p = pin.getValue();
			String t = table.getValue();
			if (p == null || !p.matches("\\d{4,6}")) {
				Notification.show("Please enter a 4-6 digit PIN");
				return;
			}
			try {
				String url = "/pincode-login?pincode=" + java.net.URLEncoder.encode(p, "UTF-8");
				if (t != null && !t.isBlank()) url += "&table=" + java.net.URLEncoder.encode(t, "UTF-8");
				url += "&redirect=/customer";
				UI.getCurrent().getPage().open(url, "_self");
			} catch (Exception ex) {
				Notification.show("Failed to initiate login");
			}
		});
		Button guest = new Button("Continue as Guest", evt -> {
			String t = table.getValue();
			try {
				// store seat/table in VaadinSession for unauthenticated customer flows
				com.vaadin.flow.server.VaadinSession vs = com.vaadin.flow.server.VaadinSession.getCurrent();
				if (vs != null) {
					vs.setAttribute("customer_seat", (t == null || t.isBlank()) ? null : t.trim());
				}
				dialog.close();
				// navigate to menu and include seat query param for deep-linking
				try {
					if (t == null || t.isBlank()) {
						UI.getCurrent().navigate("menu");
					} else {
						String q = java.net.URLEncoder.encode(t.trim(), "UTF-8");
						UI.getCurrent().navigate("menu?seat=" + q);
					}
				} catch (Exception ex2) {
					// fallback to simple navigate
					UI.getCurrent().navigate("menu");
				}
			} catch (Exception ex) {
				Notification.show("Failed to continue as guest");
			}
		});
		Button cancel = new Button("Cancel", e -> dialog.close());
		submit.addClassName("pos-button-large");
		guest.addClassName("pos-button-large");
		cancel.addClassName("pos-button-large");
		content.add(table, pin, new HorizontalLayout(submit, guest, cancel));
		dialog.add(content);
		dialog.open();
	}
} 

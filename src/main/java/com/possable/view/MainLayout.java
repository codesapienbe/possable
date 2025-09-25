package com.possable.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

	public MainLayout() {
		DrawerToggle toggle = new DrawerToggle();
		H1 title = new H1("Possable POS");
		title.getStyle().set("margin", "0");

		HorizontalLayout header = new HorizontalLayout(toggle, title);
		header.setAlignItems(Alignment.CENTER);
		addToNavbar(header);

		Tabs menu = new Tabs();
		menu.setOrientation(Tabs.Orientation.VERTICAL);
		menu.add(createTab("Dashboard", DashboardView.class), createTab("Items", ItemListView.class), createTab("Orders", OrderView.class));
		addToDrawer(menu);
	}

	private Tab createTab(String text, Class<? extends Component> navigationTarget) {
		RouterLink link = new RouterLink(text, navigationTarget);
		Tab tab = new Tab(link);
		return tab;
	}
} 
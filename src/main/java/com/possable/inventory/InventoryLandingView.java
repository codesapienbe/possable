package com.possable.inventory;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "inventory", layout = InventoryMainLayout.class)
@PageTitle("Inventory")
public class InventoryLandingView extends VerticalLayout {

    public InventoryLandingView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Inventory Module"));

        VerticalLayout links = new VerticalLayout();
        try { Class.forName("com.possable.inventory.customer.InventoryCustomerView"); links.add(new RouterLink("Customer Menu", com.possable.inventory.customer.InventoryCustomerView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.inventory.service.InventoryServiceView"); links.add(new RouterLink("Service View", com.possable.inventory.service.InventoryServiceView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.inventory.kitchen.InventoryKitchenView"); links.add(new RouterLink("Kitchen View", com.possable.inventory.kitchen.InventoryKitchenView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.inventory.cashier.InventoryCashierView"); links.add(new RouterLink("Cashier View", com.possable.inventory.cashier.InventoryCashierView.class)); } catch (ClassNotFoundException ignore) {}

        add(links);
    }
} 
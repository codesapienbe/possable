package com.possable.inventory.cashier;

import com.possable.inventory.InventoryFacade;
import com.possable.inventory.InventoryMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "inventory/cashier", layout = InventoryMainLayout.class)
@PageTitle("Inventory - Cashier")
public class InventoryCashierView extends VerticalLayout {

    private final InventoryFacade inventoryFacade;

    public InventoryCashierView(InventoryFacade inventoryFacade) {
        this.inventoryFacade = inventoryFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Cashier Inventory"));
        add(new Paragraph("Lookup items for cashier workflows."));
    }
} 
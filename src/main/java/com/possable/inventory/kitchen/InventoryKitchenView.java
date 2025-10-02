package com.possable.inventory.kitchen;

import com.possable.inventory.InventoryFacade;
import com.possable.inventory.InventoryMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "inventory/kitchen", layout = InventoryMainLayout.class)
@PageTitle("Inventory - Kitchen")
public class InventoryKitchenView extends VerticalLayout {

    private final InventoryFacade inventoryFacade;

    public InventoryKitchenView(InventoryFacade inventoryFacade) {
        this.inventoryFacade = inventoryFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Kitchen Inventory"));
        add(new Paragraph("Prep notes and ingredient hints for kitchen staff."));
    }
} 
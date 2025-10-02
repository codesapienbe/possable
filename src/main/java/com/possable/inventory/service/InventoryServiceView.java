package com.possable.inventory.service;

import java.util.ArrayList;
import java.util.List;

import com.possable.inventory.InventoryFacade;
import com.possable.inventory.InventoryMainLayout;
import com.possable.order.OrderFacade;
import com.possable.print.PrintFacade;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "inventory/service", layout = InventoryMainLayout.class)
@PageTitle("Inventory - Service")
@PreAuthorize("hasAnyRole('SERVICE','MANAGEMENT')")
public class InventoryServiceView extends VerticalLayout {

    private final InventoryFacade inventoryFacade;
    private final OrderFacade orderFacade;
    private final PrintFacade printFacade;

    public InventoryServiceView(InventoryFacade inventoryFacade, OrderFacade orderFacade, PrintFacade printFacade) {
        this.inventoryFacade = inventoryFacade;
        this.orderFacade = orderFacade;
        this.printFacade = printFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Service Inventory"));
        add(new Span("Quick access to item availability and add-to-order shortcuts."));

        // local cart for service creation
        List<String> cart = new ArrayList<>();

        VerticalLayout itemsContainer = new VerticalLayout();
        itemsContainer.setSpacing(true);

        TextField table = new TextField("Table/Seat (optional)");
        table.setWidth("220px");
        Button createForTable = new Button("Create Order for Table", ev -> {
            if (cart.isEmpty()) { Notification.show("No items selected"); return; }
            var created = orderFacade.createOrder(new ArrayList<>(cart), "Order from service (table: " + table.getValue() + ")");
            // request print jobs for common categories via print facade
            var printers = printFacade.listPrinters(java.util.Map.of());
            var templates = printFacade.listTemplates(java.util.Map.of());
            int createdJobs = 0;
            for (var p : printers) {
                var tpl = templates.stream().filter(t -> t.printerCategory().equals(p.category())).findFirst();
                if (tpl.isPresent()) { printFacade.createJob(created.id(), p.id(), tpl.get().id()); createdJobs++; }
            }
            cart.clear();
            Notification.show("Order " + created.id() + " created. Print jobs: " + createdJobs);
        });

        add(new HorizontalLayout(table, createForTable));

        var items = inventoryFacade.listItems(200);
        for (var it : items) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
            Span name = new Span(it.name() + " — " + String.format("$%.2f", it.price()));
            Button add = new Button("Add", ev -> { cart.add(it.id()); Notification.show("Added"); });
            row.add(name, add);
            itemsContainer.add(row);
        }

        add(itemsContainer);
    }
} 
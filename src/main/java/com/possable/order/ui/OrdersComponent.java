package com.possable.order.ui;

import com.possable.checkout.CheckoutFacade;
import com.possable.inventory.InventoryFacade;
import com.possable.order.OrderFacade;
import com.possable.print.PrintFacade;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Vaadin component for displaying orders.
 * Uses module facades for all business operations.
 */
public class OrdersComponent extends VerticalLayout {

    private final OrderFacade orderFacade;
    private final Grid<OrderFacade.OrderInfo> grid = new Grid<>(OrderFacade.OrderInfo.class, false);
    private final OrdersDetailComponent details;
    private final InventoryFacade inventoryFacade;
    private final CheckoutFacade checkoutFacade;

    public OrdersComponent(
            OrderFacade orderFacade, 
            PrintFacade printFacade, 
            InventoryFacade inventoryFacade, 
            CheckoutFacade checkoutFacade) {
        this.orderFacade = orderFacade;
        this.inventoryFacade = inventoryFacade;
        this.checkoutFacade = checkoutFacade;
        this.details = new OrdersDetailComponent(orderFacade, printFacade, inventoryFacade, checkoutFacade);
        
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setHeightFull();
        addClassName("pos-split");
        
        grid.addClassName("left");
        details.addClassName("right");
        
        grid.addColumn(OrderFacade.OrderInfo::id).setHeader("ID");
        grid.addColumn(o -> o.status() == null ? "" : o.status()).setHeader("Status");
        grid.addColumn(o -> Integer.toString(o.items() == null ? 0 : o.items().size())).setHeader("Items");
        grid.setItems(orderFacade.listOrders());
        
        // ensure the grid stretches to full available width and takes priority in layout
        grid.setWidthFull();
        grid.setHeightFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(details::showOrder));

        // inline quick actions for faster workflow (open / request payment / mark ready / void)
        grid.addComponentColumn(o -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            Button open = new Button("Open", evt -> details.showOrder(o));
            
            Button reqPay = new Button("Req Pay", evt -> {
                if (checkoutFacade != null) {
                    double amount = 0.0; // calculate using item facade
                    if (o.items() != null) {
                        for (String id : o.items()) { 
                            var it = inventoryFacade.findById(id); 
                            if (it != null) amount += it.price(); 
                        }
                    }
                    checkoutFacade.createPayment(o.id(), amount, "card");
                    Notification.show("Payment requested for $" + String.format("%.2f", amount));
                }
            });
            
            Button ready = new Button("Ready", evt -> {
                String prev = o.status();
                orderFacade.updateStatus(o.id(), "READY");
                Notification n = new Notification();
                n.setDuration(8000);
                Button undo = new Button("Undo", u -> { 
                    orderFacade.updateStatus(o.id(), prev); 
                    n.close(); 
                    refresh();
                });
                n.add(new HorizontalLayout(new Span("Marked READY"), undo));
                n.open();
                refresh();
            });
            
            Button voidBtn = new Button("Void", evt -> {
                String prev = o.status();
                orderFacade.updateStatus(o.id(), "VOID");
                Notification n = new Notification();
                n.setDuration(8000);
                Button undo = new Button("Undo", u -> { 
                    orderFacade.updateStatus(o.id(), prev); 
                    n.close(); 
                    refresh();
                });
                n.add(new HorizontalLayout(new Span("Order voided"), undo));
                n.open();
                refresh();
            });
            
            actions.add(open, reqPay, ready, voidBtn);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);
        
        add(grid, details);
        // expand the grid so it fills available vertical/horizontal space within this component
        setFlexGrow(2, grid);
        setFlexGrow(1, details);
        expand(grid);
    }

    public void refresh() {
        grid.setItems(orderFacade.listOrders());
    }
} 

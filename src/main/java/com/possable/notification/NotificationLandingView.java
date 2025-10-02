package com.possable.notification;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "notification", layout = NotificationMainLayout.class)
@PageTitle("Notifications")
public class NotificationLandingView extends VerticalLayout {

    public NotificationLandingView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Notification Module"));

        VerticalLayout links = new VerticalLayout();
        try { Class.forName("com.possable.notification.customer.NotificationCustomerView"); links.add(new RouterLink("Customer Notifications", com.possable.notification.customer.NotificationCustomerView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.notification.service.NotificationServiceView"); links.add(new RouterLink("Service Notifications", com.possable.notification.service.NotificationServiceView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.notification.kitchen.NotificationKitchenView"); links.add(new RouterLink("Kitchen Notifications", com.possable.notification.kitchen.NotificationKitchenView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.notification.cashier.NotificationCashierView"); links.add(new RouterLink("Cashier Notifications", com.possable.notification.cashier.NotificationCashierView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.notification.management.NotificationManagementView"); links.add(new RouterLink("Manage Notifications", com.possable.notification.management.NotificationManagementView.class)); } catch (ClassNotFoundException ignore) {}

        add(links);
    }
} 
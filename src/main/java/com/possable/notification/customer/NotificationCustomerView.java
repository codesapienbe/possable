package com.possable.notification.customer;

import com.possable.notification.NotificationFacade;
import com.possable.notification.NotificationMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "notification/customer", layout = NotificationMainLayout.class)
@PageTitle("Notifications - Customer")
@PreAuthorize("permitAll()")
public class NotificationCustomerView extends VerticalLayout {

    private final NotificationFacade notificationFacade;

    public NotificationCustomerView(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Customer Notifications"));
        add(new Paragraph("Notifications targeted to the customer (e.g., order ready)."));
    }
} 
package com.possable.notification.cashier;

import org.springframework.security.access.prepost.PreAuthorize;
import com.possable.notification.NotificationFacade;
import com.possable.notification.NotificationMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "notification/cashier", layout = NotificationMainLayout.class)
@PageTitle("Notifications - Cashier")
@PreAuthorize("hasAnyRole('CASHIER','MANAGEMENT')")
public class NotificationCashierView extends VerticalLayout {

    private final NotificationFacade notificationFacade;

    public NotificationCashierView(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Cashier Notifications"));
        add(new Paragraph("Notifications relevant to cashier workflows."));
    }
} 
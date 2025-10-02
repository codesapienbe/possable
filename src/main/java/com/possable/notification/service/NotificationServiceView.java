package com.possable.notification.service;

import org.springframework.security.access.prepost.PreAuthorize;
import com.possable.notification.NotificationFacade;
import com.possable.notification.NotificationMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "notification/service", layout = NotificationMainLayout.class)
@PageTitle("Notifications - Service")
@PreAuthorize("hasAnyRole('SERVICE','MANAGEMENT')")
public class NotificationServiceView extends VerticalLayout {

    private final NotificationFacade notificationFacade;

    public NotificationServiceView(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Service Notifications"));
        add(new Paragraph("Notifications panel for service staff (e.g., order ready alerts)."));
    }
} 
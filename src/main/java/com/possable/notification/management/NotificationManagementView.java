package com.possable.notification.management;

import org.springframework.security.access.prepost.PreAuthorize;
import com.possable.notification.NotificationFacade;
import com.possable.notification.NotificationMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "notification/management", layout = NotificationMainLayout.class)
@PageTitle("Notification Management")
@PreAuthorize("hasRole('MANAGEMENT')")
public class NotificationManagementView extends VerticalLayout {

    private final NotificationFacade notificationFacade;

    public NotificationManagementView(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Notification Management"));
        add(new Paragraph("Manage notification templates and channels."));
    }
} 
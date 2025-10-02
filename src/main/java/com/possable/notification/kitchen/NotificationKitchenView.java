package com.possable.notification.kitchen;

import org.springframework.security.access.prepost.PreAuthorize;
import com.possable.notification.NotificationFacade;
import com.possable.notification.NotificationMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "notification/kitchen", layout = NotificationMainLayout.class)
@PageTitle("Notifications - Kitchen")
@PreAuthorize("hasAnyRole('KITCHEN','MANAGEMENT')")
public class NotificationKitchenView extends VerticalLayout {

    private final NotificationFacade notificationFacade;

    public NotificationKitchenView(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Kitchen Notifications"));
        add(new Paragraph("Notifications for kitchen staff (e.g., new orders to prepare)."));
    }
} 
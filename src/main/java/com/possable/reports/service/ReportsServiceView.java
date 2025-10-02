package com.possable.reports.service;

import com.possable.reports.ReportsMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "reports/service", layout = ReportsMainLayout.class)
@PageTitle("Reports - Service")
@PreAuthorize("hasAnyRole('SERVICE','MANAGEMENT')")
public class ReportsServiceView extends VerticalLayout {

    public ReportsServiceView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Service Reports"));
        add(new Paragraph("Operational reports useful for service staff (e.g., recent orders by table)."));
    }
} 
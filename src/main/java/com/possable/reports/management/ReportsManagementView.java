package com.possable.reports.management;

import com.possable.reports.ReportsMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "reports/management", layout = ReportsMainLayout.class)
@PageTitle("Reports")
@PreAuthorize("hasRole('MANAGEMENT')")
public class ReportsManagementView extends VerticalLayout {

    public ReportsManagementView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Reports"));
        add(new Paragraph("Placeholder for management reports and dashboards."));
    }
} 
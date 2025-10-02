package com.possable.reports.kitchen;

import com.possable.reports.ReportsMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "reports/kitchen", layout = ReportsMainLayout.class)
@PageTitle("Reports - Kitchen")
@PreAuthorize("hasAnyRole('KITCHEN','MANAGEMENT')")
public class ReportsKitchenView extends VerticalLayout {

    public ReportsKitchenView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Kitchen Reports"));
        add(new Paragraph("Preparation and throughput reports for kitchen staff."));
    }
} 
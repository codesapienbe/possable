package com.possable.reports.cashier;

import com.possable.reports.ReportsMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "reports/cashier", layout = ReportsMainLayout.class)
@PageTitle("Reports - Cashier")
@PreAuthorize("hasAnyRole('CASHIER','MANAGEMENT')")
public class ReportsCashierView extends VerticalLayout {

    public ReportsCashierView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Cashier Reports"));
        add(new Paragraph("Financial and payment reports for cashier staff."));
    }
} 
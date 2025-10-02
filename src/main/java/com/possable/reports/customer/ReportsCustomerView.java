package com.possable.reports.customer;

import com.possable.reports.ReportsMainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "reports/customer", layout = ReportsMainLayout.class)
@PageTitle("Reports - Customer")
@PreAuthorize("permitAll()")
public class ReportsCustomerView extends VerticalLayout {

    public ReportsCustomerView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Customer Reports"));
        add(new Paragraph("Lightweight reports or receipts view for customers."));
    }
} 
package com.possable.reports;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "reports", layout = ReportsMainLayout.class)
@PageTitle("Reports")
public class ReportsLandingView extends VerticalLayout {

    public ReportsLandingView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Reports Module"));

        VerticalLayout links = new VerticalLayout();
        try { Class.forName("com.possable.reports.customer.ReportsCustomerView"); links.add(new RouterLink("Customer Reports", com.possable.reports.customer.ReportsCustomerView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.reports.service.ReportsServiceView"); links.add(new RouterLink("Service Reports", com.possable.reports.service.ReportsServiceView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.reports.kitchen.ReportsKitchenView"); links.add(new RouterLink("Kitchen Reports", com.possable.reports.kitchen.ReportsKitchenView.class)); } catch (ClassNotFoundException ignore) {}
        try { Class.forName("com.possable.reports.cashier.ReportsCashierView"); links.add(new RouterLink("Cashier Reports", com.possable.reports.cashier.ReportsCashierView.class)); } catch (ClassNotFoundException ignore) {}

        add(links);
    }
} 
package com.possable.print;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "print", layout = PrintMainLayout.class)
@PageTitle("Print")
public class PrintLandingView extends VerticalLayout {

    public PrintLandingView() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Print Module"));

        VerticalLayout links = new VerticalLayout();
        try { Class.forName("com.possable.print.manager.PrintManagerView"); links.add(new RouterLink("Print Manager", com.possable.print.manager.PrintManagerView.class)); } catch (ClassNotFoundException ignore) {}
        // other print-related views can be linked similarly
        add(links);
    }
} 
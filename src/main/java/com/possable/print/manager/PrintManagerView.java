package com.possable.print.manager;

import java.util.List;
import java.util.Map;

import com.possable.print.PrintFacade;
import com.possable.print.PrintMainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.prepost.PreAuthorize;

@Route(value = "print/manager", layout = PrintMainLayout.class)
@PageTitle("Print Manager")
@PreAuthorize("hasRole('MANAGEMENT')")
public class PrintManagerView extends VerticalLayout {

    private final PrintFacade printFacade;

    public PrintManagerView(PrintFacade printFacade) {
        this.printFacade = printFacade;
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new H1("Print Manager"));
        add(new Paragraph("Manage printers and templates."));

        // Printers
        VerticalLayout printersList = new VerticalLayout();
        printersList.setSpacing(true);
        add(new H1("Printers"));
        add(printersList);

        // form to register a printer
        HorizontalLayout printerForm = new HorizontalLayout();
        TextField pName = new TextField("Name");
        TextField pCategory = new TextField("Category");
        TextField pDesc = new TextField("Description");
        Button register = new Button("Register", e -> {
            try {
                var pi = printFacade.registerPrinter(pName.getValue(), pCategory.getValue(), pDesc.getValue());
                Notification.show("Printer registered: " + pi.id());
                pName.clear(); pCategory.clear(); pDesc.clear();
                refreshPrinters(printersList);
            } catch (Exception ex) {
                Notification.show("Failed to register printer");
            }
        });
        printerForm.add(pName, pCategory, pDesc, register);
        add(printerForm);

        // Templates
        VerticalLayout templatesList = new VerticalLayout();
        templatesList.setSpacing(true);
        add(new H1("Templates"));
        add(templatesList);

        HorizontalLayout templateForm = new HorizontalLayout();
        TextField tCategory = new TextField("Printer Category");
        TextField tName = new TextField("Template Name");
        TextArea tContent = new TextArea("Content");
        tContent.setWidth("480px");
        Button createTpl = new Button("Create Template", e -> {
            try {
                var ti = printFacade.createTemplate(tCategory.getValue(), tName.getValue(), tContent.getValue());
                Notification.show("Template created: " + ti.id());
                tCategory.clear(); tName.clear(); tContent.clear();
                refreshTemplates(templatesList);
            } catch (Exception ex) {
                Notification.show("Failed to create template");
            }
        });
        templateForm.add(tCategory, tName, createTpl);
        add(templateForm);

        refreshPrinters(printersList);
        refreshTemplates(templatesList);
    }

    private void refreshPrinters(VerticalLayout printersList) {
        printersList.removeAll();
        try {
            List<PrintFacade.PrinterInfo> printers = printFacade.listPrinters(Map.of());
            for (var p : printers) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
                row.add(new Paragraph(p.name() + " (" + p.category() + ")"));
                printersList.add(row);
            }
        } catch (Exception ex) {
            printersList.add(new Paragraph("Failed to load printers"));
        }
    }

    private void refreshTemplates(VerticalLayout templatesList) {
        templatesList.removeAll();
        try {
            List<PrintFacade.TemplateInfo> templates = printFacade.listTemplates(Map.of());
            for (var t : templates) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
                row.add(new Paragraph(t.templateName() + " (" + t.printerCategory() + ")"));
                templatesList.add(row);
            }
        } catch (Exception ex) {
            templatesList.add(new Paragraph("Failed to load templates"));
        }
    }
} 
package com.possable.print;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Print module seeder. Enabled with `app.seed.print.enable=true`.
 * Uses public `PrintFacade` API to register printers and templates.
 */
@Component
@ConditionalOnProperty(name = "app.seed.print.enable", havingValue = "true", matchIfMissing = false)
public class PrintSeeder {

    private static final Logger log = LoggerFactory.getLogger(PrintSeeder.class);
    private final PrintFacade printFacade;

    public PrintSeeder(PrintFacade printFacade) {
        this.printFacade = printFacade;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            var printers = printFacade.listPrinters(java.util.Map.of());
            if (printers != null && printers.size() >= 2) {
                log.info("{\"message\":\"print_seeder_skipped\", \"count\":{} }", printers.size());
                return;
            }

            var k = printFacade.registerPrinter("Kitchen Printer 1", "kitchen", "Primary kitchen receipt printer");
            var p = printFacade.registerPrinter("Cashier Printer 1", "cashier", "Front-desk receipt printer");
            printFacade.createTemplate("kitchen", "kitchen-receipt", "Kitchen Copy - Order: {{orderId}}\\nJob: {{jobId}}\\n---\\n");
            printFacade.createTemplate("cashier", "customer-receipt", "Customer Copy - Order: {{orderId}}\\nThank you!\\n");

            log.info("{\"message\":\"print_seeder_completed\", \"printer_ids\":\"{},{}\"}", k.id(), p.id());
        } catch (Exception ex) {
            log.error("{\"message\":\"print_seeder_failed\", \"error\":\"{}\"}", sanitize(ex.getMessage()), ex);
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
} 
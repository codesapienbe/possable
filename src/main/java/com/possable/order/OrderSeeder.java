package com.possable.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.possable.inventory.InventoryFacade;
import com.possable.print.PrintFacade;

/**
 * Order module seeder. Enabled with `app.seed.order.enable=true`.
 * Uses `OrderFacade` (public API) to create orders; reads items via `InventoryFacade`.
 */
@Component
@ConditionalOnProperty(name = "app.seed.order.enable", havingValue = "true", matchIfMissing = false)
public class OrderSeeder {

    private static final Logger log = LoggerFactory.getLogger(OrderSeeder.class);
    private final OrderFacade orderFacade;
    private final InventoryFacade inventoryFacade;
    private final PrintFacade printFacade;
    private final Random rnd = new Random();

    public OrderSeeder(OrderFacade orderFacade, InventoryFacade inventoryFacade, PrintFacade printFacade) {
        this.orderFacade = orderFacade;
        this.inventoryFacade = inventoryFacade;
        this.printFacade = printFacade;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            var existing = orderFacade.listOrders();
            if (existing != null && existing.size() >= 5) {
                log.info("{\"message\":\"order_seeder_skipped\", \"count\":{} }", existing.size());
                return;
            }

            var items = inventoryFacade.listItems(100);
            if (items == null || items.isEmpty()) {
                log.warn("{\"message\":\"order_seeder_no_items\"}");
                return;
            }

            List<String> itemIds = items.stream().map(i -> i.id()).collect(Collectors.toList());
            int ordersToCreate = Math.min(10, Math.max(1, itemIds.size() / 2));

            for (int i = 0; i < ordersToCreate; i++) {
                Set<String> chosen = new HashSet<>();
                int count = 1 + rnd.nextInt(Math.min(4, itemIds.size()));
                while (chosen.size() < count) {
                    chosen.add(itemIds.get(rnd.nextInt(itemIds.size())));
                }
                var dto = orderFacade.createOrder(new ArrayList<>(chosen), "Seeded order " + (i + 1));
                log.info("{\"message\":\"order_seeded\", \"order_id\":\"{}\", \"items_count\":{} }", dto.id(), chosen.size());

                // request a print job if printers/templates exist
                try {
                    var printers = printFacade.listPrinters(java.util.Map.of("category", "kitchen"));
                    var templates = printFacade.listTemplates(java.util.Map.of("category", "kitchen"));
                    if (!printers.isEmpty() && !templates.isEmpty()) {
                        printFacade.createJob(dto.id(), printers.get(0).id(), templates.get(0).id());
                    }
                } catch (Exception ignore) {}
            }

            log.info("{\"message\":\"order_seeder_completed\", \"timestamp\":\"{}\"}", Instant.now());
        } catch (Exception ex) {
            log.error("{\"message\":\"order_seeder_failed\", \"error\":\"{}\"}", sanitize(ex.getMessage()), ex);
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
} 
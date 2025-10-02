package com.possable.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Inventory module seeder. Enabled with `app.seed.inventory.enable=true`.
 * Uses `InventoryFacade` (public API) to create items so module encapsulation is preserved.
 */
@Component
@ConditionalOnProperty(name = "app.seed.inventory.enable", havingValue = "true", matchIfMissing = false)
public class InventorySeeder {

    private static final Logger log = LoggerFactory.getLogger(InventorySeeder.class);
    private final InventoryFacade inventory;
    private final Random rnd = new Random(12345);

    public InventorySeeder(InventoryFacade inventory) {
        this.inventory = inventory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            var items = inventory.listItems(1000);
            if (items != null && items.size() >= 10) {
                log.info("{\"message\":\"inventory_seeder_skipped\", \"count\":{} }", items.size());
                return;
            }

            // realistic menu samples with categories and tags
            record Sample(String name, String category, String tagsCsv, double basePrice) {}
            List<Sample> samples = List.of(
                new Sample("Chicken Wings", "starter", "spicy,share", 8.0),
                new Sample("French Fries", "starter", "vegan,snack", 4.5),
                new Sample("Summer Salad", "starter", "vegetarian,light", 6.5),
                new Sample("Beef Burger", "main", "grill,hearty", 12.0),
                new Sample("Grilled Salmon", "main", "seafood,chef-special", 15.0),
                new Sample("Pancakes", "dessert", "sweet,vegetarian", 6.0),
                new Sample("Chocolate Cake", "dessert", "sweet,share", 7.0),
                new Sample("Cappuccino", "drinks", "hot,coffee", 3.0),
                new Sample("Smoothie", "drinks", "cold,fruit", 4.0),
                new Sample("Tacos", "main", "spicy,street-food", 9.0)
            );

            for (Sample s : samples) {
                String desc = s.name() + " - handcrafted and fresh";
                double price = s.basePrice + (rnd.nextInt(50) / 10.0);
                var created = inventory.createItem(s.name(), desc, price, true);
                try { inventory.updateMetadata(created.id(), s.category(), s.tagsCsv()); } catch (Exception ignore) {}
                log.info("{\"message\":\"inventory_item_seeded\", \"name\":\"{}\", \"category\":\"{}\", \"tags\":\"{}\", \"price\":{} }", s.name(), s.category(), s.tagsCsv(), price);
            }

            log.info("{\"message\":\"inventory_seeder_completed\", \"timestamp\":\"{}\"}", Instant.now());
        } catch (Exception ex) {
            log.error("{\"message\":\"inventory_seeder_failed\", \"error\":\"{}\"}", sanitize(ex.getMessage()), ex);
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
} 
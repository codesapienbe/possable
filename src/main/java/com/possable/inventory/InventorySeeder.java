package com.possable.inventory;

import java.time.Instant;
import java.util.Arrays;
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
    private final Random rnd = new Random();

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

            List<String> sampleNames = Arrays.asList("Margherita Pizza", "Pepperoni Pizza", "Caesar Salad", "Cheeseburger", "Fries", "Cappuccino", "Espresso", "Sushi Roll", "Tacos", "Pancakes", "Omelette", "Grilled Salmon", "Steak Sandwich", "Veggie Bowl", "Chicken Wings", "Onion Rings", "Chocolate Cake", "Ice Cream Scoop", "Smoothie", "Lemonade");
            for (int i = 0; i < Math.min(sampleNames.size(), 20); i++) {
                String name = sampleNames.get(i);
                String desc = name + " - handcrafted food item";
                double price = 2.5 + rnd.nextInt(200) / 10.0;
                inventory.createItem(name, desc, price, true);
                log.info("{\"message\":\"inventory_item_seeded\", \"name\":\"{}\", \"price\":{} }", name, price);
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
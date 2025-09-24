package com.possable.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final List<Item> items = Collections.synchronizedList(new ArrayList<>());

    public record Item(String id, String name, String description, double price, boolean available, Instant createdAt) {}

    public Item createItem(String name, String description, double price, boolean available) {
        String id = UUID.randomUUID().toString();
        Item it = new Item(id, name, description, price, available, Instant.now());
        items.add(it);
        log.info("{\"message\":\"item_created\", \"item_id\":\"{}\", \"component\":\"item-service\"}", id);
        return it;
    }

    public List<Item> listItems(int limit) {
        synchronized (items) {
            return List.copyOf(items).stream().limit(Math.max(0, Math.min(limit, items.size()))).toList();
        }
    }

    public Item findById(String id) {
        synchronized (items) {
            return items.stream().filter(i -> i.id().equals(id)).findFirst().orElse(null);
        }
    }

    public Item updateItem(String id, String name, String description, double price, boolean available) {
        synchronized (items) {
            for (int i = 0; i < items.size(); i++) {
                var it = items.get(i);
                if (it.id().equals(id)) {
                    Item updated = new Item(id, name, description, price, available, it.createdAt());
                    items.set(i, updated);
                    log.info("{\"message\":\"item_updated\", \"item_id\":\"{}\", \"component\":\"item-service\"}", id);
                    return updated;
                }
            }
            return null;
        }
    }

    public boolean deleteItem(String id) {
        synchronized (items) {
            return items.removeIf(i -> i.id().equals(id));
        }
    }
} 
package com.possable.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ItemServiceTest {

    @Test
    public void createListFindUpdateDelete() {
        ItemService svc = new ItemService();

        var it = svc.createItem("Item1", "desc", 9.99, true);
        assertNotNull(it);
        assertEquals("Item1", it.name());

        var list = svc.listItems(10);
        assertTrue(list.stream().anyMatch(i -> i.id().equals(it.id())));

        var found = svc.findById(it.id());
        assertNotNull(found);

        var updated = svc.updateItem(it.id(), "Item1b", "desc2", 19.99, false);
        assertNotNull(updated);
        assertEquals("Item1b", updated.name());

        boolean removed = svc.deleteItem(it.id());
        assertTrue(removed);
        assertNull(svc.findById(it.id()));
    }

    @Test
    public void updateAndDeleteNonExistent() {
        ItemService svc = new ItemService();
        var upd = svc.updateItem("no-such-id", "n", "d", 1.0, true);
        assertNull(upd);
        boolean removed = svc.deleteItem("no-such-id");
        assertFalse(removed);
    }
} 
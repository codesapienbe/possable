package com.possable.inventory.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Item repository - INTERNAL to inventory module.
 * Only inventory module can write to items table.
 */
interface ItemRepository extends JpaRepository<ItemEntity, String> {
    Page<ItemEntity> findAll(Pageable pageable);
    Page<ItemEntity> findByCategory(String category, Pageable pageable);
} 
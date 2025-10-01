package com.possable.order.internal;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Order item repository - INTERNAL to order module.
 */
interface OrderItemRepository extends JpaRepository<OrderItemEntity, String> {
} 
/**
 * Order Module - Handles order creation, management, and lifecycle.
 * 
 * <p>Public API: {@link com.possable.order.OrderFacade}
 * <p>Events Published:
 * <ul>
 *   <li>{@link com.possable.order.OrderCreatedEvent} - when an order is created</li>
 *   <li>{@link com.possable.order.OrderCompletedEvent} - when an order is completed</li>
 * </ul>
 * 
 * <p>Module Dependencies: None (core module)
 * 
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Order Management",
    allowedDependencies = {}
)
package com.possable.order; 
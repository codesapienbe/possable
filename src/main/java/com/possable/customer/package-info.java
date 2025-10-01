/**
 * Customer Module - Handles customer-related operations and notifications.
 * 
 * <p>Public API: {@link com.possable.customer.CustomerFacade}
 * <p>Events Published: None
 * <p>Events Consumed:
 * <ul>
 *   <li>{@link com.possable.order.OrderCreatedEvent} - to notify customers</li>
 *   <li>{@link com.possable.checkout.PaymentCompletedEvent} - to send receipts</li>
 * </ul>
 * 
 * <p>Module Dependencies: order, checkout (via events only)
 * 
 * @see org.springframework.modulith.ApplicationModule
 */
package com.possable.customer; 
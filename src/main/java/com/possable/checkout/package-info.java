/**
 * Checkout Module - Handles payment processing and checkout operations.
 * 
 * <p>Public API: {@link com.possable.checkout.CheckoutFacade}
 * <p>Events Published:
 * <ul>
 *   <li>{@link com.possable.checkout.PaymentCompletedEvent} - when a payment is completed</li>
 * </ul>
 * <p>Events Consumed:
 * <ul>
 *   <li>{@link com.possable.order.OrderCompletedEvent} - to initiate payment processing</li>
 * </ul>
 * 
 * <p>Module Dependencies: order (via events only)
 * 
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.Module(name = "checkout")
package com.possable.checkout; 
/**
 * Print Module - Handles printer management, templates, and print job processing.
 * 
 * <p>Public API: {@link com.possable.print.PrintFacade}
 * <p>Events Consumed:
 * <ul>
 *   <li>{@link com.possable.order.OrderCreatedEvent} - to auto-create print jobs</li>
 *   <li>{@link com.possable.print.PrintJobRequestedEvent} - to create print jobs on demand</li>
 * </ul>
 * 
 * <p>Module Dependencies: order (via events only)
 * 
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Print Management",
    allowedDependencies = {"order"}
)
package com.possable.print; 
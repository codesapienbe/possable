package com.possable.config;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.possable.print.PrintFacade;
import com.possable.service.DemoNotificationService;

@Configuration
public class DemoDataConfig {

	private static final Logger log = LoggerFactory.getLogger(DemoDataConfig.class);

	@Bean
	public ApplicationRunner demoDataInitializer(PrintFacade printFacade, org.springframework.context.ApplicationContext applicationContext, DemoNotificationService demoNotificationService) {
		return args -> {
			// Resolve optional module facades reflectively to avoid compile-time module exposure
			Object orderFacade = null;
			Object inventoryFacade = null;
			try {
				Class<?> orderFacadeClass = Class.forName("com.possable.order.OrderFacade");
				orderFacade = applicationContext.getBean(orderFacadeClass);
			} catch (ClassNotFoundException e) {
				log.debug("Order module not present on classpath; skipping demo order setup");
			} catch (Exception e) {
				log.warn("Failed to obtain OrderFacade bean reflectively", e);
			}
			try {
				Class<?> inventoryFacadeClass = Class.forName("com.possable.inventory.InventoryFacade");
				inventoryFacade = applicationContext.getBean(inventoryFacadeClass);
			} catch (ClassNotFoundException e) {
				log.debug("Inventory module not present on classpath; skipping demo item setup");
			} catch (Exception e) {
				log.warn("Failed to obtain InventoryFacade bean reflectively", e);
			}

			// ensure printers/templates exist
			if (!printFacade.listPrinters(Map.of()).isEmpty() || !printFacade.listTemplates(Map.of()).isEmpty()) {
				log.info("demo data not created because printers/templates already exist");
			} else {
				var k = printFacade.registerPrinter("Kitchen Printer", "kitchen", "Main kitchen order printer");
				var b = printFacade.registerPrinter("Bar Printer", "bar", "Bar drinks printer");
				var r = printFacade.registerPrinter("Receipt Printer", "receipt", "Customer receipts");

				printFacade.createTemplate("kitchen", "kitchen-default", "KITCHEN\nOrder: {{orderId}}\nItems: {{items}}\nNotes: {{notes}}");
				printFacade.createTemplate("bar", "bar-default", "BAR\nOrder: {{orderId}}\nItems: {{items}}");
				printFacade.createTemplate("receipt", "receipt-default", "RECEIPT\nOrder: {{orderId}}\nTotal: {{total}}\nThank you!");

				log.info("demo printers and templates registered: {}, {}, {}", k.id(), b.id(), r.id());
			}

			// create demo items so frontend shows a menu (if inventory module available)
			if (inventoryFacade != null) {
				try {
					var listItemsPagedMethod = inventoryFacade.getClass().getMethod("listItemsPaged", Map.class);
					Map<?,?> itemsPaged = (Map<?,?>) listItemsPagedMethod.invoke(inventoryFacade, Map.of("limit", "200"));
					Object itemsObj = itemsPaged.get("items");
					boolean itemsEmpty = itemsObj == null || ((List<?>)itemsObj).isEmpty();
					if (itemsEmpty) {
						var createItemMethod = inventoryFacade.getClass().getMethod("createItem", String.class, String.class, double.class, boolean.class);
						var adana = createItemMethod.invoke(inventoryFacade, "Adana Kebab", "Kebab", 12.50, true);
						var urfa = createItemMethod.invoke(inventoryFacade, "Urfa Kebab", "Kebab", 12.00, true);
						var iskender = createItemMethod.invoke(inventoryFacade, "Iskender Kebab", "Kebab", 14.00, true);
						var donerWrap = createItemMethod.invoke(inventoryFacade, "Döner Wrap", "Döner", 9.50, true);
						var chickenPide = createItemMethod.invoke(inventoryFacade, "Cheese & Chicken Pide", "Pide", 11.00, true);
						var mixedPide = createItemMethod.invoke(inventoryFacade, "Mixed Pide (beef & cheese)", "Pide", 12.00, true);
						var margherita = createItemMethod.invoke(inventoryFacade, "Pizza Margherita", "Pizza", 11.00, true);
						var pepperoni = createItemMethod.invoke(inventoryFacade, "Pizza Pepperoni", "Pizza", 12.50, true);
						var lahmacun = createItemMethod.invoke(inventoryFacade, "Lahmacun (Turkish thin pizza)", "Lahmacun", 6.00, true);
						var turkishCoffee = createItemMethod.invoke(inventoryFacade, "Turkish Coffee", "Beverage", 2.50, true);
						var turkishTea = createItemMethod.invoke(inventoryFacade, "Turkish Tea", "Beverage", 1.50, true);
						var ayran = createItemMethod.invoke(inventoryFacade, "Ayran", "Beverage", 2.00, true);
						var baklava = createItemMethod.invoke(inventoryFacade, "Baklava", "Dessert", 4.50, true);
						var kunefe = createItemMethod.invoke(inventoryFacade, "Künefe", "Dessert", 5.50, true);
						var rice = createItemMethod.invoke(inventoryFacade, "Pilav (Rice)", "Side", 3.00, true);
						var shepherdSalad = createItemMethod.invoke(inventoryFacade, "Shepherd Salad", "Side", 4.50, true);
						var fries = createItemMethod.invoke(inventoryFacade, "Fries", "Side", 3.00, true);
						var created = List.of(adana, urfa, iskender, donerWrap, chickenPide, mixedPide, margherita, pepperoni, lahmacun, turkishCoffee, turkishTea, ayran, baklava, kunefe, rice, shepherdSalad, fries);
						// attempt to extract id() if available for logging
						try {
							var idMethod = created.get(0).getClass().getMethod("id");
							log.info("created demo items: {}", created.stream().map(i -> {
								try { return (String) idMethod.invoke(i); } catch (Exception ex) { return i.toString(); }
							}).toList());
						} catch (NoSuchMethodException nsme) {
							log.info("created demo items (unable to reflect ids)");
						}
					}
				} catch (Exception e) {
					log.error("Failed to create demo items reflectively", e);
				}
			} else {
				log.debug("InventoryFacade not available; skipping demo items");
			}

			// create a sample order if none exists - demonstrates end-to-end printing (if order module available)
			if (orderFacade != null) {
				try {
					var listOrdersMethod = orderFacade.getClass().getMethod("listOrders");
					@SuppressWarnings("unchecked")
					var existingOrders = (java.util.List<Object>) listOrdersMethod.invoke(orderFacade);
					if (existingOrders == null || existingOrders.isEmpty()) {
						List<String> menu = List.of();
						if (inventoryFacade != null) {
							try {
								var listItemsPagedMethod = inventoryFacade.getClass().getMethod("listItemsPaged", Map.class);
								Map<?,?> itemsPaged = (Map<?,?>) listItemsPagedMethod.invoke(inventoryFacade, Map.of("limit", "200"));
								Object itemsObj = itemsPaged.get("items");
								menu = ((List<?>)itemsObj).stream().map(o -> {
									var m = (java.util.Map<String,Object>)o; return (String)m.get("id");
								}).toList();
							} catch (Exception e) {
								log.warn("Failed to read inventory items reflectively; using fallback demo items", e);
							}
						}
						List<String> orderItems = menu.size() >= 2 ? List.of(menu.get(0), menu.get(1)) : List.of("demo-item-1", "demo-item-2");
						var createOrderMethod = orderFacade.getClass().getMethod("createOrder", List.class, String.class);
						Object sampleOrder = createOrderMethod.invoke(orderFacade, orderItems, "Demo order: please ignore");

						// attempt to read id() and items() reflectively
						String sampleOrderId = null;
						int itemsSize = -1;
						try {
							var idMethod = sampleOrder.getClass().getMethod("id");
							sampleOrderId = (String) idMethod.invoke(sampleOrder);
						} catch (Exception e) {
							log.debug("Unable to extract sampleOrder.id() reflectively", e);
						}
						try {
							var itemsMethod = sampleOrder.getClass().getMethod("items");
							@SuppressWarnings("unchecked")
							var itemsList = (List<?>) itemsMethod.invoke(sampleOrder);
							itemsSize = itemsList == null ? 0 : itemsList.size();
						} catch (Exception e) {
							log.debug("Unable to extract sampleOrder.items() reflectively", e);
						}
						log.info("created demo order {} with {} items", sampleOrderId == null ? "<unknown>" : sampleOrderId, itemsSize >= 0 ? itemsSize : "<unknown>");

						var templates = printFacade.listTemplates(Map.of());
						int created = 0;
						for (var p : printFacade.listPrinters(Map.of())) {
							var tpl = templates.stream().filter(t -> t.printerCategory().equals(p.category())).findFirst();
							if (tpl.isPresent()) {
								if (sampleOrderId != null) {
									printFacade.createJob(sampleOrderId, p.id(), tpl.get().id());
									created++;
								}
							}
						}
						if (created > 0) {
							try {
								var updateStatusMethod = orderFacade.getClass().getMethod("updateStatus", String.class, String.class);
								if (sampleOrderId != null) updateStatusMethod.invoke(orderFacade, sampleOrderId, "IN_PREPARATION");
								log.info("created {} print jobs for demo order {}", created, sampleOrderId == null ? "<unknown>" : sampleOrderId);
							} catch (Exception e) {
								log.warn("Failed to update demo order status reflectively", e);
							}
						}
						if (sampleOrderId != null) demoNotificationService.setStartupMessage("Demo order created: " + sampleOrderId);
					}
				} catch (Exception e) {
					log.error("Failed to create demo order reflectively", e);
				}
			} else {
				log.debug("OrderFacade not available; skipping demo order creation");
			}
		};
	}
} 

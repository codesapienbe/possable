package com.possable.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.possable.service.OrderService;
import com.possable.service.PrintJobService;
import com.possable.service.PrintTemplateService;
import com.possable.service.PrinterService;

@Configuration
@Profile("demo") // only enable when spring profile 'demo' is active
public class DemoDataConfig {

	private static final Logger log = LoggerFactory.getLogger(DemoDataConfig.class);

	@Bean
	public ApplicationRunner demoDataInitializer(PrinterService printerService, PrintTemplateService templateService, OrderService orderService, PrintJobService printJobService, com.possable.service.DemoNotificationService demoNotificationService) {
		return args -> {
			if (!printerService.listPrinters().isEmpty() || !templateService.listTemplates().isEmpty()) {
				log.info("demo data not created because printers/templates already exist");
			} else {
				var k = printerService.registerPrinter("Kitchen Printer", "kitchen", "Main kitchen order printer");
				var b = printerService.registerPrinter("Bar Printer", "bar", "Bar drinks printer");
				var r = printerService.registerPrinter("Receipt Printer", "receipt", "Customer receipts");

				templateService.createTemplate("kitchen", "kitchen-default", "KITCHEN\nOrder: {{orderId}}\nItems: {{items}}\nNotes: {{notes}}");
				templateService.createTemplate("bar", "bar-default", "BAR\nOrder: {{orderId}}\nItems: {{items}}");
				templateService.createTemplate("receipt", "receipt-default", "RECEIPT\nOrder: {{orderId}}\nTotal: {{total}}\nThank you!");

				log.info("demo printers and templates registered: {}, {}, {}", k.id(), b.id(), r.id());
			}

			// create a sample order if none exists - this demonstrates end-to-end printing
			if (orderService.listOrders().isEmpty()) {
				var sampleOrder = orderService.createOrder(List.of("demo-item-1", "demo-item-2"), "Demo order: please ignore");
				log.info("created demo order {} with {} items", sampleOrder.getId(), sampleOrder.getItems().size());

				// create print jobs for printers that have matching templates
				var templates = templateService.listTemplates();
				int created = 0;
				for (var p : printerService.listPrinters()) {
					var tpl = templates.stream().filter(t -> t.printerCategory().equals(p.category())).findFirst();
					if (tpl.isPresent()) {
						printJobService.createJob(sampleOrder.getId(), p.id(), tpl.get().id());
						created++;
					}
				}
				if (created > 0) {
					orderService.updateStatus(sampleOrder.getId(), "IN_PREPARATION");
					log.info("created {} print jobs for demo order {}", created, sampleOrder.getId());
				}
				// set demo startup message for UI
				demoNotificationService.setStartupMessage("Demo order created: " + sampleOrder.getId());
			}
		};
	}
} 
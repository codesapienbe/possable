package com.possable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        // Force Vaadin into production mode to prevent dev-mode/Vite dev server from starting inside the JVM
        System.setProperty("vaadin.productionMode", "true");
        // Best-effort additional flag some Vaadin integrations honor to skip dev server
        System.setProperty("vaadin.disableDevServer", "true");

        log.info("Starting application with vaadin.productionMode={} vaadin.disableDevServer={}",
                System.getProperty("vaadin.productionMode"), System.getProperty("vaadin.disableDevServer"));

        SpringApplication.run(Application.class, args);
    }
} 
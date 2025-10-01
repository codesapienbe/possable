package com.possable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        System.setProperty("vaadin.productionMode", "true");
        System.setProperty("vaadin.disableDevServer", "true");
        SpringApplication.run(Application.class, args);
    }
} 
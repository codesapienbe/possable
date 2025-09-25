package com.possable.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "app.docs.enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerUiConfig implements WebMvcConfigurer {

	@Value("${springdoc.swagger-ui.config-url:/swagger-config.json}")
	private String swaggerConfigUrl;

	@Bean
	public org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath dispatcher() {
		return null; // placeholder to avoid unused warnings; no-op bean
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// serve static swagger-config.json from classpath:/static/
		registry.addResourceHandler("/swagger-config.json").addResourceLocations("classpath:/static/swagger-config.json");
	}
} 
package com.possable.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String API_KEY_SCHEME = "ApiKeyAuth";
	private static final String BEARER_SCHEME = "BearerAuth";

	@Bean
	public OpenAPI customOpenAPI() {
		SecurityScheme apiKeyScheme = new SecurityScheme()
			.type(SecurityScheme.Type.APIKEY)
			.in(SecurityScheme.In.HEADER)
			.name("X-API-KEY")
			.description("API Key needed to access the endpoints (provide in X-API-KEY header)");

		SecurityScheme bearerScheme = new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT")
			.description("Bearer token authentication (use Authorization: Bearer <token>)");

		Components components = new Components()
			.addSecuritySchemes(API_KEY_SCHEME, apiKeyScheme)
			.addSecuritySchemes(BEARER_SCHEME, bearerScheme);

		SecurityRequirement requirement = new SecurityRequirement().addList(API_KEY_SCHEME);

		return new OpenAPI()
			.components(components)
			.addSecurityItem(requirement)
			.info(new Info().title("Possable API").version("0.1.0"));
	}
} 
package com.possable.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@ConditionalOnProperty(name = "app.docs.enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerRedirectController {

	@GetMapping("/docs")
	public RedirectView redirectToSwagger() {
		return new RedirectView("/swagger-ui/index.html");
	}
} 
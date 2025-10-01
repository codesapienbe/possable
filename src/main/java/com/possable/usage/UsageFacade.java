package com.possable.usage;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.possable.usage.internal.UsageModuleService;

@Service
public class UsageFacade {

	private final UsageModuleService usageService;

	public UsageFacade(UsageModuleService usageService) {
		this.usageService = usageService;
	}

	public Map<String,Object> getUsage() {
		return Map.of(
			"monthlyLimit", usageService.getMonthlyLimit(),
			"requestsMade", usageService.getRequestsMade(),
			"resetAt", usageService.getResetAt()
		);
	}

} 
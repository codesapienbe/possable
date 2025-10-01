package com.possable.service;

import org.springframework.stereotype.Service;

@Service
public class DemoNotificationService {

	private String startupMessage;

	public synchronized void setStartupMessage(String message) {
		this.startupMessage = message;
	}

	public synchronized String consumeStartupMessage() {
		String m = this.startupMessage;
		this.startupMessage = null;
		return m;
	}
} 
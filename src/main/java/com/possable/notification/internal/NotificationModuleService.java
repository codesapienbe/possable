package com.possable.notification.internal;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class NotificationModuleService {

    private final AtomicReference<String> startupMessage = new AtomicReference<>();

    public String consumeStartupMessage() {
        return startupMessage.getAndSet(null);
    }

    public void setStartupMessage(String message) {
        startupMessage.set(message);
    }

} 
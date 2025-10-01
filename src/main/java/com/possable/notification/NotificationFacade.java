package com.possable.notification;

import org.springframework.stereotype.Service;

import com.possable.notification.internal.NotificationModuleService;

@Service
public class NotificationFacade {

    private final NotificationModuleService notificationModuleService;

    public NotificationFacade(NotificationModuleService notificationModuleService) {
        this.notificationModuleService = notificationModuleService;
    }

    public String consumeStartupMessage() {
        return notificationModuleService.consumeStartupMessage();
    }

    public void setStartupMessage(String message) {
        notificationModuleService.setStartupMessage(message);
    }

} 
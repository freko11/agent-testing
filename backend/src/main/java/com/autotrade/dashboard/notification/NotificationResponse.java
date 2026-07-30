package com.autotrade.dashboard.notification;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        Long id,
        String tickerSymbol,
        NotificationType eventType,
        String message,
        Instant readAt,
        Instant createdAt) {

    static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getTicker().getSymbol(),
                notification.getEventType(), notification.getMessage(), notification.getReadAt(),
                notification.getCreatedAt());
    }
}

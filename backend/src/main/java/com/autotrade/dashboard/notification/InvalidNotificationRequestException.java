package com.autotrade.dashboard.notification;

/** A malformed request to the notification endpoints (e.g. {@code limit} out of bounds) — 400 {@code INVALID_REQUEST}. */
public class InvalidNotificationRequestException extends RuntimeException {

    public InvalidNotificationRequestException(String message) {
        super(message);
    }
}

package com.autotrade.dashboard.alert;

/** A malformed request to the system-alerts endpoint (e.g. {@code limit} out of bounds) — 400 {@code INVALID_REQUEST}. */
public class InvalidSystemAlertRequestException extends RuntimeException {

    public InvalidSystemAlertRequestException(String message) {
        super(message);
    }
}

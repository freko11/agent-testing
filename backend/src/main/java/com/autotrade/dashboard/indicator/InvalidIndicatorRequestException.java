package com.autotrade.dashboard.indicator;

/** The indicator request itself is malformed (e.g. limit out of bounds) — not a provider or data-availability fault. */
public class InvalidIndicatorRequestException extends RuntimeException {

    public InvalidIndicatorRequestException(String message) {
        super(message);
    }
}

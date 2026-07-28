package com.autotrade.dashboard.marketdata;

/** The price-history request itself is malformed (e.g. limit out of bounds) — not a provider fault. */
public class InvalidPriceHistoryRequestException extends RuntimeException {

    public InvalidPriceHistoryRequestException(String message) {
        super(message);
    }
}

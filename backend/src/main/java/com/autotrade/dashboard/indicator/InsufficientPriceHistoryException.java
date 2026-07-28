package com.autotrade.dashboard.indicator;

/** Ticker has some price history, but not enough candles to compute a valid indicator set — distinct from zero data. */
public class InsufficientPriceHistoryException extends RuntimeException {

    public InsufficientPriceHistoryException(String message) {
        super(message);
    }
}

package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.broker.Broker;

/** Provider unreachable, timed out, or returned a server error even after the one retry — or is unconfigured (e.g. missing Alpaca credentials). */
public class MarketDataUnavailableException extends RuntimeException {

    private final Broker source;

    public MarketDataUnavailableException(Broker source, String message) {
        super(message);
        this.source = source;
    }

    public Broker source() {
        return source;
    }
}

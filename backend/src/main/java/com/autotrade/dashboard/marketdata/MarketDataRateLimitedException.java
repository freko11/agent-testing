package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.broker.Broker;

/** Provider rate-limited the request even after the one retry. */
public class MarketDataRateLimitedException extends RuntimeException {

    private final Broker source;
    private final Long retryAfterSeconds;

    public MarketDataRateLimitedException(Broker source, Long retryAfterSeconds) {
        super("Rate limited by " + source);
        this.source = source;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Broker source() {
        return source;
    }

    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}

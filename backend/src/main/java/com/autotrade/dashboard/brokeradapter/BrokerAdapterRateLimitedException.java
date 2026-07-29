package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;

/**
 * A {@link BrokerAdapterException} indicating the broker explicitly rejected
 * a call for rate-limiting — the AC's "distinct, user-visible state" rather
 * than a generic failure. Mirrors {@code MarketDataRateLimitedException}'s
 * shape. Unlike a {@link BrokerAdapterTransientException}, a rate-limit
 * response is unambiguous: the broker definitely processed and definitively
 * rejected the call, so {@link RetryingBrokerAdapter} retries this on every
 * method, including {@code placeOrder} (safe because {@code clientOrderId}
 * replay is already idempotent).
 */
public class BrokerAdapterRateLimitedException extends BrokerAdapterException {

    private final Long retryAfterSeconds;

    public BrokerAdapterRateLimitedException(Broker source, Long retryAfterSeconds) {
        super(source, "Rate limited by " + source);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}

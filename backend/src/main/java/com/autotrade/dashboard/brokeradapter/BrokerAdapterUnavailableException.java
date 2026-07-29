package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;

/**
 * Thrown by {@link RetryingBrokerAdapter} — never by a concrete adapter —
 * when a {@link BrokerAdapterTransientException} is not retried further
 * (attempts exhausted, or {@code placeOrder}'s never-retry-transient rule
 * applies) and, for {@code placeOrder} specifically, reconciliation via
 * {@code getOrderStatus} has confirmed the order never reached the broker.
 * This is the distinct "broker unavailable" state the AC calls for — a
 * future order-submission controller maps this to a "broker unavailable"
 * UI state, the same way {@code MarketDataUnavailableException} maps to
 * {@code MARKET_DATA_UNAVAILABLE}. Safe to retry later with the same
 * {@code clientOrderId} (still idempotent per E4-F1-S1).
 */
public class BrokerAdapterUnavailableException extends BrokerAdapterException {

    public BrokerAdapterUnavailableException(Broker source, Throwable cause) {
        super(source, source + " unavailable after retries exhausted", cause);
    }
}

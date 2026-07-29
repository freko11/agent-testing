package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;

/**
 * A {@link BrokerAdapter} call failed for a transport/infrastructure reason
 * (network unreachable, auth failure, malformed response) rather than a
 * normal business outcome (which is returned as a result value, never
 * thrown). Mirrors {@code MarketDataUnavailableException}'s shape. Fatal and
 * never retried by {@link RetryingBrokerAdapter} when thrown as this plain
 * base type — throw one of its subtypes, {@link BrokerAdapterTransientException}
 * or {@link BrokerAdapterRateLimitedException}, when a failure is actually
 * retryable.
 *
 * <p>Concrete adapters must only ever throw the plain type, {@link
 * BrokerAdapterTransientException}, or {@link BrokerAdapterRateLimitedException}.
 * {@link BrokerAdapterUnavailableException} and {@link
 * BrokerAdapterAmbiguousOrderException} are synthesized exclusively by
 * {@link RetryingBrokerAdapter} (E4-F1-S3) — an adapter throwing either
 * directly would bypass reconciliation entirely and be treated as an
 * immediately-fatal, non-retried failure.
 */
public class BrokerAdapterException extends RuntimeException {

    private final Broker source;

    public BrokerAdapterException(Broker source, String message) {
        super(message);
        this.source = source;
    }

    public BrokerAdapterException(Broker source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public Broker source() {
        return source;
    }
}

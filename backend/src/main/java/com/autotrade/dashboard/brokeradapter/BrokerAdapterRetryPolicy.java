package com.autotrade.dashboard.brokeradapter;

import java.time.Duration;

/**
 * Attempt count and exponential-backoff bounds for {@link
 * RetryingBrokerAdapter}. {@code defaultPolicy()}'s numbers are provisional
 * engineering defaults, not sourced from Alpaca's/Binance's actual
 * documented rate-limit windows — revisit once F4.2/F4.3 research those.
 */
public record BrokerAdapterRetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay) {

    public static BrokerAdapterRetryPolicy defaultPolicy() {
        return new BrokerAdapterRetryPolicy(3, Duration.ofMillis(250), Duration.ofSeconds(2));
    }
}

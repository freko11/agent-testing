package com.autotrade.dashboard.marketdata;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

/**
 * One bounded retry (single retry, short fixed delay) for transient
 * market-data failures — I/O errors, 5xx, 429. Deliberately not a pluggable
 * resilience framework: E4-F1-S2 owns the trading-path retry/backoff
 * contract, which has different requirements (idempotency keys) that don't
 * apply to a read-only GET. Whether to consolidate is that story's call.
 */
final class RetryHelper {

    private static final long RETRY_DELAY_MILLIS = 250;

    private RetryHelper() {
    }

    static <T> T withOneRetry(Supplier<T> call) {
        try {
            return call.get();
        } catch (ResourceAccessException | HttpServerErrorException | HttpClientErrorException.TooManyRequests firstFailure) {
            sleepBriefly();
            return call.get();
        }
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.ticker.AssetType;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Decorates any {@link BrokerAdapter} with retry/backoff, applied uniformly
 * so concrete adapters (F4.2/F4.3) never reimplement it themselves — each
 * adapter's only job is to classify its raw transport failures into {@link
 * BrokerAdapterTransientException}/{@link BrokerAdapterRateLimitedException}
 * when it throws.
 *
 * <p>Retry policy differs by method. Non-mutating calls ({@link
 * #getOrderStatus}, {@link #getPosition}, {@link #getAccountStatus}) and
 * {@link #cancelOrder} retry both transient and rate-limited failures.
 * {@link #placeOrder} retries only rate-limited failures — a rate-limit
 * response is unambiguous (the broker definitely rejected the call, and
 * {@code clientOrderId} replay is already idempotent per E4-F1-S1), but a
 * generic transient failure is ambiguous: the broker may have already
 * received the order before the connection dropped. Guaranteeing no
 * duplicate submission under that ambiguity is E4-F1-S3's explicit scope,
 * not this story's — so {@code placeOrder} never auto-retries a transient
 * failure. A plain (non-subtype) {@link BrokerAdapterException} is always
 * fatal and never retried on any method.
 */
public class RetryingBrokerAdapter implements BrokerAdapter {

    private final BrokerAdapter delegate;
    private final BrokerAdapterRetryPolicy policy;

    public RetryingBrokerAdapter(BrokerAdapter delegate) {
        this(delegate, BrokerAdapterRetryPolicy.defaultPolicy());
    }

    public RetryingBrokerAdapter(BrokerAdapter delegate, BrokerAdapterRetryPolicy policy) {
        this.delegate = delegate;
        this.policy = policy;
    }

    @Override
    public AssetType supportedAssetType() {
        return delegate.supportedAssetType();
    }

    @Override
    public Broker broker() {
        return delegate.broker();
    }

    @Override
    public BrokerOrderResult placeOrder(BrokerOrderRequest request, TradingMode mode) {
        return withRetry(() -> delegate.placeOrder(request, mode), false);
    }

    @Override
    public Optional<BrokerOrderResult> getOrderStatus(String clientOrderId, TradingMode mode) {
        return withRetry(() -> delegate.getOrderStatus(clientOrderId, mode), true);
    }

    @Override
    public Optional<BrokerPosition> getPosition(String symbol, TradingMode mode) {
        return withRetry(() -> delegate.getPosition(symbol, mode), true);
    }

    @Override
    public BrokerOrderResult cancelOrder(String clientOrderId, TradingMode mode) {
        return withRetry(() -> delegate.cancelOrder(clientOrderId, mode), true);
    }

    @Override
    public BrokerAccountStatus getAccountStatus(TradingMode mode) {
        return withRetry(() -> delegate.getAccountStatus(mode), true);
    }

    private <T> T withRetry(Supplier<T> call, boolean retryTransient) {
        int attempt = 1;
        while (true) {
            try {
                return call.get();
            } catch (BrokerAdapterRateLimitedException rateLimited) {
                Duration delay = delayFor(rateLimited, attempt);
                if (attempt >= policy.maxAttempts() || delay.compareTo(policy.maxDelay()) > 0) {
                    throw rateLimited;
                }
                sleepOrRethrow(delay, rateLimited);
                attempt++;
            } catch (BrokerAdapterTransientException transientFailure) {
                if (!retryTransient || attempt >= policy.maxAttempts()) {
                    throw transientFailure;
                }
                sleepOrRethrow(backoffFor(attempt), transientFailure);
                attempt++;
            }
        }
    }

    private Duration delayFor(BrokerAdapterRateLimitedException rateLimited, int attempt) {
        Long retryAfterSeconds = rateLimited.retryAfterSeconds();
        return retryAfterSeconds != null ? Duration.ofSeconds(retryAfterSeconds) : backoffFor(attempt);
    }

    private Duration backoffFor(int attempt) {
        Duration backoff = policy.baseDelay().multipliedBy(1L << (attempt - 1));
        return backoff.compareTo(policy.maxDelay()) > 0 ? policy.maxDelay() : backoff;
    }

    private void sleepOrRethrow(Duration delay, BrokerAdapterException lastFailure) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw lastFailure;
        }
    }
}

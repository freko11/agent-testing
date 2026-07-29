package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers {@link RetryingBrokerAdapter}'s per-method retry policy — not part
 * of {@link BrokerAdapterContractTest}'s shared shape-only suite. Uses a
 * short-delay policy so exhaustion cases run fast.
 */
class RetryingBrokerAdapterTest {

    private static final BrokerAdapterRetryPolicy FAST_POLICY =
            new BrokerAdapterRetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(20));

    private final MockBrokerAdapter mock = new MockBrokerAdapter(Broker.ALPACA, AssetType.STOCK);
    private final RetryingBrokerAdapter retrying = new RetryingBrokerAdapter(mock, FAST_POLICY);

    @Test
    void transientFailureOnAReadRetriesAndSucceeds() {
        mock.failNextCallWith(new BrokerAdapterTransientException(Broker.ALPACA, "timeout"));

        BrokerAccountStatus status = retrying.getAccountStatus(TradingMode.PAPER);

        assertEquals(Broker.ALPACA, status.broker());
    }

    @Test
    void transientFailureExhaustingAllAttemptsPropagatesOnARead() {
        mock.failNextCallsWith(new BrokerAdapterTransientException(Broker.ALPACA, "timeout"), FAST_POLICY.maxAttempts());

        assertThrows(BrokerAdapterTransientException.class, () -> retrying.getPosition("AAPL", TradingMode.PAPER));
    }

    @Test
    void rateLimitedFailureOnAReadRetriesAndSucceeds() {
        mock.failNextCallWith(new BrokerAdapterRateLimitedException(Broker.ALPACA, null));

        BrokerAccountStatus status = retrying.getAccountStatus(TradingMode.PAPER);

        assertEquals(Broker.ALPACA, status.broker());
    }

    @Test
    void rateLimitedFailureOnPlaceOrderRetriesAndSucceeds() {
        mock.failNextCallWith(new BrokerAdapterRateLimitedException(Broker.ALPACA, null));

        BrokerOrderResult result = retrying.placeOrder(buyRequest("order-1"), TradingMode.PAPER);

        assertEquals("order-1", result.clientOrderId());
    }

    @Test
    void rateLimitedFailureExhaustingAllAttemptsPropagatesWithRetryAfterSecondsPreserved() {
        mock.failNextCallsWith(new BrokerAdapterRateLimitedException(Broker.ALPACA, 1L), FAST_POLICY.maxAttempts());

        BrokerAdapterRateLimitedException thrown = assertThrows(BrokerAdapterRateLimitedException.class,
                () -> retrying.getAccountStatus(TradingMode.PAPER));
        assertEquals(1L, thrown.retryAfterSeconds());
    }

    @Test
    void rateLimitedFailureWithRetryAfterSecondsBeyondMaxDelayStopsImmediately() {
        mock.failNextCallWith(new BrokerAdapterRateLimitedException(Broker.ALPACA, 3600L));

        assertThrows(BrokerAdapterRateLimitedException.class, () -> retrying.getAccountStatus(TradingMode.PAPER));
    }

    @Test
    void placeOrderDoesNotRetryATransientFailure() {
        mock.failNextCallWith(new BrokerAdapterTransientException(Broker.ALPACA, "timeout"));

        assertThrows(BrokerAdapterTransientException.class, () -> retrying.placeOrder(buyRequest("order-2"), TradingMode.PAPER));
    }

    @Test
    void fatalFailureIsNeverRetriedEvenOnAReadThatWouldOtherwiseRetry() {
        mock.failNextCallWith(new BrokerAdapterException(Broker.ALPACA, "unrecoverable"));

        assertThrows(BrokerAdapterException.class, () -> retrying.getAccountStatus(TradingMode.PAPER));
    }

    private static BrokerOrderRequest buyRequest(String clientOrderId) {
        return new BrokerOrderRequest(
                clientOrderId, "AAPL", AssetType.STOCK, OrderSide.BUY, new BigDecimal("10"),
                EntryOrderType.MARKET, null, new BigDecimal("220"), new BigDecimal("180"), BigDecimal.ONE);
    }
}

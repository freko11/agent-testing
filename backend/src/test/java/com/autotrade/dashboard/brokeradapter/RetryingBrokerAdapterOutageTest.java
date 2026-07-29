package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers E4-F1-S3's outage/reconciliation behavior in {@link
 * RetryingBrokerAdapter} — kept separate from {@link RetryingBrokerAdapterTest}
 * (plain retry/backoff, E4-F1-S2) since this is a distinct behavior area.
 */
class RetryingBrokerAdapterOutageTest {

    private static final BrokerAdapterRetryPolicy FAST_POLICY =
            new BrokerAdapterRetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(20));

    private final MockBrokerAdapter mock = new MockBrokerAdapter(Broker.ALPACA, AssetType.STOCK);
    private final RetryingBrokerAdapter retrying = new RetryingBrokerAdapter(mock, FAST_POLICY);

    @Test
    void readCallTransientExhaustionWrapsIntoUnavailableWithCausePreserved() {
        BrokerAdapterTransientException original = new BrokerAdapterTransientException(Broker.ALPACA, "timeout");
        mock.failNextCallsWith(original, FAST_POLICY.maxAttempts());

        BrokerAdapterUnavailableException thrown = assertThrows(BrokerAdapterUnavailableException.class,
                () -> retrying.getAccountStatus(TradingMode.PAPER));

        assertEquals(original, thrown.getCause());
    }

    @Test
    void placeOrderWhereOrderNeverReachedBrokerThrowsUnavailable() {
        mock.failNextCallWith(new BrokerAdapterTransientException(Broker.ALPACA, "timeout"));

        BrokerAdapterUnavailableException thrown = assertThrows(BrokerAdapterUnavailableException.class,
                () -> retrying.placeOrder(buyRequest("outage-1"), TradingMode.PAPER));

        assertEquals(BrokerAdapterTransientException.class, thrown.getCause().getClass());
    }

    @Test
    void placeOrderWhereBrokerActuallySucceededReturnsRealResultWithNoDuplicate() {
        mock.simulateLostResponseOnNextPlaceOrder(new BrokerAdapterTransientException(Broker.ALPACA, "response lost"));

        BrokerOrderResult result = retrying.placeOrder(buyRequest("outage-2"), TradingMode.PAPER);

        assertEquals("outage-2", result.clientOrderId());
        assertEquals(OrderStatus.FILLED, result.status());

        Optional<BrokerPosition> position = retrying.getPosition("AAPL", TradingMode.PAPER);
        assertTrue(position.isPresent());
        assertEquals(new BigDecimal("10"), position.orElseThrow().quantity());
    }

    @Test
    void placeOrderWhereReconciliationAlsoFailsThrowsAmbiguousOrderException() {
        BrokerAdapterTransientException failure = new BrokerAdapterTransientException(Broker.ALPACA, "timeout");
        // 1 for the initial placeOrder attempt (never retried) + maxAttempts for the
        // reconciliation getOrderStatus probe, which retries transient failures itself.
        mock.failNextCallsWith(failure, 1 + FAST_POLICY.maxAttempts());

        BrokerAdapterAmbiguousOrderException thrown = assertThrows(BrokerAdapterAmbiguousOrderException.class,
                () -> retrying.placeOrder(buyRequest("outage-3"), TradingMode.PAPER));

        assertEquals("outage-3", thrown.clientOrderId());
        assertEquals(BrokerAdapterUnavailableException.class, thrown.getCause().getClass());
    }

    @Test
    void retryingSamePlaceOrderRequestAfterAmbiguousOutcomeResolvesIdempotently() {
        BrokerAdapterTransientException failure = new BrokerAdapterTransientException(Broker.ALPACA, "timeout");
        mock.failNextCallsWith(failure, 1 + FAST_POLICY.maxAttempts());
        assertThrows(BrokerAdapterAmbiguousOrderException.class,
                () -> retrying.placeOrder(buyRequest("outage-4"), TradingMode.PAPER));

        BrokerOrderResult retried = retrying.placeOrder(buyRequest("outage-4"), TradingMode.PAPER);

        assertEquals("outage-4", retried.clientOrderId());
        assertEquals(OrderStatus.FILLED, retried.status());

        Optional<BrokerPosition> position = retrying.getPosition("AAPL", TradingMode.PAPER);
        assertTrue(position.isPresent());
        assertEquals(new BigDecimal("10"), position.orElseThrow().quantity());
    }

    @Test
    void rateLimitExhaustionIsNeverWrappedIntoUnavailableOrAmbiguous() {
        mock.failNextCallsWith(new BrokerAdapterRateLimitedException(Broker.ALPACA, null), FAST_POLICY.maxAttempts());

        assertThrows(BrokerAdapterRateLimitedException.class, () -> retrying.getAccountStatus(TradingMode.PAPER));
    }

    private static BrokerOrderRequest buyRequest(String clientOrderId) {
        return new BrokerOrderRequest(
                clientOrderId, "AAPL", AssetType.STOCK, OrderSide.BUY, new BigDecimal("10"),
                EntryOrderType.MARKET, null, new BigDecimal("220"), new BigDecimal("180"), BigDecimal.ONE);
    }
}

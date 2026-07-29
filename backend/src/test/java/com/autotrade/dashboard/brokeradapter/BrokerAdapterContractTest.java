package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared contract every {@link BrokerAdapter} implementation must satisfy —
 * plumbing/shape correctness only. Retry/backoff behavior is covered
 * separately by {@code RetryingBrokerAdapterTest} and {@code
 * RetryingMockBrokerAdapterContractTest} (E4-F1-S2); outage-simulation
 * behavior is still not tested here (E4-F1-S3's job to add once designed).
 * Subclasses supply a concrete adapter and enough request-building detail to
 * run these assertions against it; a real-adapter subclass (F4.2/F4.3) may
 * need to skip assertions that don't apply against a live paper API, and
 * should also run this same suite wrapped in {@code RetryingBrokerAdapter}
 * per {@code RetryingMockBrokerAdapterContractTest}'s template.
 */
public abstract class BrokerAdapterContractTest {

    protected abstract BrokerAdapter adapter();

    protected abstract TradingMode tradingMode();

    protected abstract String tradableSymbol();

    protected abstract BrokerOrderRequest sampleBuyOrderRequest(String clientOrderId);

    @Test
    void placeOrderReturnsResultMatchingRequestedClientOrderId() {
        String clientOrderId = newClientOrderId();
        BrokerOrderResult result = adapter().placeOrder(sampleBuyOrderRequest(clientOrderId), tradingMode());

        assertEquals(clientOrderId, result.clientOrderId());
    }

    @Test
    void placeOrderIsIdempotentForARepeatedClientOrderId() {
        String clientOrderId = newClientOrderId();
        BrokerOrderRequest request = sampleBuyOrderRequest(clientOrderId);

        BrokerOrderResult first = adapter().placeOrder(request, tradingMode());
        BrokerOrderResult second = adapter().placeOrder(request, tradingMode());

        assertEquals(first.brokerOrderId(), second.brokerOrderId());
    }

    @Test
    void getOrderStatusForAnUnknownClientOrderIdReturnsEmpty() {
        Optional<BrokerOrderResult> result = adapter().getOrderStatus(newClientOrderId(), tradingMode());

        assertTrue(result.isEmpty());
    }

    @Test
    void getPositionForASymbolWithNoActivityReturnsEmpty() {
        Optional<BrokerPosition> position = adapter().getPosition(tradableSymbol() + "-NOACTIVITY", tradingMode());

        assertTrue(position.isEmpty());
    }

    @Test
    void cancelOrderOnAnOpenOrderSucceedsWithoutThrowing() {
        String clientOrderId = newClientOrderId();
        adapter().placeOrder(sampleBuyOrderRequest(clientOrderId), tradingMode());

        assertDoesNotThrow(() -> adapter().cancelOrder(clientOrderId, tradingMode()));
    }

    @Test
    void cancelOrderOnAnUnknownClientOrderIdReturnsAResultRatherThanThrowing() {
        BrokerOrderResult result = adapter().cancelOrder(newClientOrderId(), tradingMode());

        assertEquals(OrderStatus.FAILED, result.status());
    }

    @Test
    void getAccountStatusReturnsNonEmptyBalances() {
        BrokerAccountStatus status = adapter().getAccountStatus(tradingMode());

        assertNotNull(status);
        assertFalse(status.balances().isEmpty());
    }

    private static String newClientOrderId() {
        return "contract-test-" + UUID.randomUUID();
    }
}

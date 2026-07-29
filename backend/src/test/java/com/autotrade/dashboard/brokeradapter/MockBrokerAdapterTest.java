package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Behaviors specific to {@link MockBrokerAdapter} — not part of the shared adapter contract. */
class MockBrokerAdapterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC);

    private final MockBrokerAdapter autoFillAdapter =
            new MockBrokerAdapter(Broker.ALPACA, AssetType.STOCK, true, FIXED_CLOCK);
    private final MockBrokerAdapter manualFillAdapter =
            new MockBrokerAdapter(Broker.BINANCE, AssetType.CRYPTO, false, FIXED_CLOCK);

    @Test
    void autoFillTransitionsAPlacedOrderStraightToFilled() {
        BrokerOrderResult result = autoFillAdapter.placeOrder(buyRequest("order-1"), TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(new BigDecimal("200.00000000"), result.filledPrice());
    }

    @Test
    void manualFillLeavesAPlacedOrderSubmittedUntilSimulateFillIsCalled() {
        manualFillAdapter.placeOrder(buyRequest("order-2"), TradingMode.PAPER);

        Optional<BrokerOrderResult> beforeFill = manualFillAdapter.getOrderStatus("order-2", TradingMode.PAPER);
        assertEquals(OrderStatus.SUBMITTED, beforeFill.orElseThrow().status());

        manualFillAdapter.simulateFill("order-2", new BigDecimal("205"));

        Optional<BrokerOrderResult> afterFill = manualFillAdapter.getOrderStatus("order-2", TradingMode.PAPER);
        assertEquals(OrderStatus.FILLED, afterFill.orElseThrow().status());
        assertEquals(new BigDecimal("205"), afterFill.orElseThrow().filledPrice());
    }

    @Test
    void rejectNextOrderWithReturnsARejectedResultInsteadOfSubmitting() {
        autoFillAdapter.rejectNextOrderWith("insufficient buying power");

        BrokerOrderResult result = autoFillAdapter.placeOrder(buyRequest("order-3"), TradingMode.PAPER);

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals("insufficient buying power", result.rejectionReason());
        assertNull(result.brokerOrderId());
    }

    @Test
    void rejectNextOrderWithOnlyAppliesOnce() {
        autoFillAdapter.rejectNextOrderWith("insufficient buying power");
        autoFillAdapter.placeOrder(buyRequest("order-4"), TradingMode.PAPER);

        BrokerOrderResult secondOrder = autoFillAdapter.placeOrder(buyRequest("order-5"), TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, secondOrder.status());
    }

    @Test
    void failNextCallWithThrowsFromTheNextCallThenResets() {
        autoFillAdapter.failNextCallWith(new BrokerAdapterException(Broker.ALPACA, "simulated outage"));

        assertThrows(BrokerAdapterException.class, () -> autoFillAdapter.getAccountStatus(TradingMode.PAPER));

        // Reset — the following call must succeed normally.
        BrokerAccountStatus status = autoFillAdapter.getAccountStatus(TradingMode.PAPER);
        assertTrue(status.balances().size() > 0);
    }

    @Test
    void simulateLostResponseOnNextPlaceOrderRecordsTheOrderThenThrows() {
        autoFillAdapter.simulateLostResponseOnNextPlaceOrder(new BrokerAdapterTransientException(Broker.ALPACA, "response lost"));

        assertThrows(BrokerAdapterTransientException.class,
                () -> autoFillAdapter.placeOrder(buyRequest("order-11"), TradingMode.PAPER));

        Optional<BrokerOrderResult> recorded = autoFillAdapter.getOrderStatus("order-11", TradingMode.PAPER);
        assertTrue(recorded.isPresent());
        assertEquals(OrderStatus.FILLED, recorded.orElseThrow().status());
    }

    @Test
    void simulateLostResponseOnNextPlaceOrderOnlyAppliesOnce() {
        autoFillAdapter.simulateLostResponseOnNextPlaceOrder(new BrokerAdapterTransientException(Broker.ALPACA, "response lost"));
        assertThrows(BrokerAdapterTransientException.class,
                () -> autoFillAdapter.placeOrder(buyRequest("order-12"), TradingMode.PAPER));

        BrokerOrderResult secondOrder = autoFillAdapter.placeOrder(buyRequest("order-13"), TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, secondOrder.status());
    }

    @Test
    void cancelOrderOnAFilledOrderIsANoOp() {
        autoFillAdapter.placeOrder(buyRequest("order-6"), TradingMode.PAPER);

        BrokerOrderResult result = autoFillAdapter.cancelOrder("order-6", TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, result.status());
    }

    @Test
    void cancelOrderTwiceOnAnOpenOrderIsIdempotent() {
        manualFillAdapter.placeOrder(buyRequest("order-7"), TradingMode.PAPER);

        BrokerOrderResult firstCancel = manualFillAdapter.cancelOrder("order-7", TradingMode.PAPER);
        BrokerOrderResult secondCancel = manualFillAdapter.cancelOrder("order-7", TradingMode.PAPER);

        assertEquals(OrderStatus.CANCELLED, firstCancel.status());
        assertEquals(OrderStatus.CANCELLED, secondCancel.status());
    }

    @Test
    void getPositionReflectsFilledBuyQuantityAndAverageEntryPrice() {
        autoFillAdapter.placeOrder(buyRequest("order-8"), TradingMode.PAPER);

        Optional<BrokerPosition> position = autoFillAdapter.getPosition("AAPL", TradingMode.PAPER);

        assertTrue(position.isPresent());
        assertEquals(new BigDecimal("10"), position.orElseThrow().quantity());
        assertEquals(new BigDecimal("200.00000000"), position.orElseThrow().averageEntryPrice());
    }

    @Test
    void getPositionReflectsAFilledSellReducingQuantity() {
        autoFillAdapter.placeOrder(buyRequest("order-9"), TradingMode.PAPER);
        autoFillAdapter.placeOrder(sellRequest("order-10", new BigDecimal("4")), TradingMode.PAPER);

        Optional<BrokerPosition> position = autoFillAdapter.getPosition("AAPL", TradingMode.PAPER);

        assertTrue(position.isPresent());
        assertEquals(new BigDecimal("6"), position.orElseThrow().quantity());
    }

    private static BrokerOrderRequest buyRequest(String clientOrderId) {
        return new BrokerOrderRequest(
                clientOrderId, "AAPL", AssetType.STOCK, OrderSide.BUY, new BigDecimal("10"),
                EntryOrderType.MARKET, null, new BigDecimal("220"), new BigDecimal("180"), BigDecimal.ONE);
    }

    private static BrokerOrderRequest sellRequest(String clientOrderId, BigDecimal quantity) {
        return new BrokerOrderRequest(
                clientOrderId, "AAPL", AssetType.STOCK, OrderSide.SELL, quantity,
                EntryOrderType.MARKET, null, new BigDecimal("220"), new BigDecimal("180"), BigDecimal.ONE);
    }
}

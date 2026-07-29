package com.autotrade.dashboard.e2e;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.brokeradapter.BrokerOrderRequest;
import com.autotrade.dashboard.brokeradapter.BrokerOrderResult;
import com.autotrade.dashboard.brokeradapter.BrokerPosition;
import com.autotrade.dashboard.brokeradapter.MockBrokerAdapter;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.indicator.IndicatorSnapshotRepository;
import com.autotrade.dashboard.marketdata.BinanceMarketDataClient;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.Order;
import com.autotrade.dashboard.order.OrderRepository;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalResponse;
import com.autotrade.dashboard.signal.SignalService;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * E1-F4-S1: covers the full "ticker -&gt; signal -&gt; order" happy path against a mock broker
 * adapter, closing a testing-strategy gap left open since E1 (it needed E2's signal engine and
 * E4-F1-S1's {@link MockBrokerAdapter} to exist first, both now do).
 *
 * <p>E5 (Auto-Trade Execution — the real trade-input form, bracket-order construction from a
 * signal, guardrail checks) hasn't been built yet, so there is no production code that turns a
 * {@link SignalResponse} into a {@link BrokerOrderRequest}. Steps 5-6 below are a deliberate,
 * minimal, test-only stand-in for that translation — proving the plumbing wires together
 * end-to-end, not pretending to be E5's real order-construction logic (no amount sizing, no
 * leverage rules, no guardrails). This mirrors {@code com.autotrade.dashboard.backtest}'s existing
 * precedent of test-only logic standing in for a step production code doesn't own yet.
 *
 * <p>Runs against a real Spring context (H2, Oracle-compatibility mode) and real
 * {@link TickerService}/{@link SignalService}/{@link BrokerCredentialService} — driven at the
 * service layer, not through the HTTP controllers, since there's no order-submission controller
 * yet to drive it through consistently. {@link MockBrokerAdapter} is instantiated manually, never
 * a Spring bean, per its own class-level contract. Asset type is CRYPTO (not STOCK) specifically
 * to sidestep {@code MarketHoursService}'s open/closed gate — a stock ticker would make this
 * test's pass/fail depend on what time CI happens to run.
 */
@SpringBootTest
@Transactional
class TickerSignalOrderE2ETest {

    private static final String SYMBOL = "E2ECOIN";

    @Autowired
    private TickerService tickerService;

    @Autowired
    private SignalService signalService;

    @Autowired
    private BrokerCredentialService brokerCredentialService;

    @Autowired
    private IndicatorSnapshotRepository indicatorSnapshotRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * A spy, not a plain mock: {@code MarketDataService}'s constructor eagerly calls {@code
     * supportedAssetType()} on every injected client bean at context-refresh time, before any
     * {@code @Test}/stubbing code runs. A plain {@code @MockitoBean} would return null from that
     * unstubbed call and silently corrupt the CRYPTO routing entry for the whole test class. A
     * spy wraps the real, fully-constructed bean, so {@code supportedAssetType()}/{@code broker()}
     * behave normally throughout, while only {@code fetchRecentCandles} is stubbed below.
     */
    @MockitoSpyBean
    private BinanceMarketDataClient binanceMarketDataClient;

    @Test
    void tickerToSignalToOrder_happyPath() {
        // Stubbed with doReturn(...), not when(...).thenReturn(...): on a spy, when(...) invokes
        // the real method first (a real Binance HTTP call) before overriding it.
        doReturn(E2ECandleFixtures.bullishCandles())
                .when(binanceMarketDataClient).fetchRecentCandles(eq(SYMBOL), anyInt());

        // --- ticker -> ---
        Ticker ticker = tickerService.resolveOrRegister(SYMBOL, AssetType.CRYPTO, null);

        // --- -> signal -> ---
        SignalResponse signal = signalService.computeSignal(SYMBOL, 200);

        // Guards fixture drift (e.g. a future SignalRuleEngine/HoldTermCalculator threshold
        // revision) failing loudly here, rather than confusingly a few steps later.
        assertEquals(SignalCall.BUY, signal.call());
        assertNotNull(signal.holdTerm());

        IndicatorSnapshot snapshot = indicatorSnapshotRepository
                .findFirstByTickerIdOrderBySnapshotAtDesc(ticker.getId())
                .orElseThrow();

        BrokerCredential credential = brokerCredentialService.store(
                Broker.BINANCE, TradingMode.PAPER, "e2e-test-key", "e2e-test-secret");

        // --- -> order: minimal test-only SignalResponse -> BrokerOrderRequest translation ---
        // NOT real E5 order-construction logic (no amount sizing, no leverage rules, no
        // guardrails) — just enough to prove a signal can actually become a submitted order.
        String clientOrderId = "e2e-" + UUID.randomUUID();
        BigDecimal price = signal.indicators().price();
        BrokerOrderRequest request = new BrokerOrderRequest(
                clientOrderId, SYMBOL, AssetType.CRYPTO, OrderSide.BUY,
                new BigDecimal("1"), EntryOrderType.MARKET, null,
                price.multiply(new BigDecimal("1.05")),
                price.multiply(new BigDecimal("0.97")),
                BigDecimal.ONE);

        MockBrokerAdapter brokerAdapter = new MockBrokerAdapter(Broker.BINANCE, AssetType.CRYPTO);
        BrokerOrderResult result = brokerAdapter.placeOrder(request, TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, result.status());
        assertNotNull(result.filledPrice());
        assertNotNull(result.brokerOrderId());

        Order order = new Order(ticker, credential, Broker.BINANCE, AssetType.CRYPTO, OrderSide.BUY,
                request.quantity(), request.takeProfitPrice(), request.stopLossPrice(), clientOrderId);
        order.setIndicatorSnapshot(snapshot);
        order.setOrderMode(TradingMode.PAPER);
        order.setEntryOrderType(EntryOrderType.MARKET);
        order.setEntryPrice(result.filledPrice());
        order.setBrokerOrderId(result.brokerOrderId());
        order.setStatus(result.status());
        order.setSubmittedAt(Instant.now());
        order.setFilledAt(Instant.now());
        orderRepository.saveAndFlush(order);

        Order persisted = orderRepository.findByClientOrderId(clientOrderId).orElseThrow();
        assertEquals(ticker.getId(), persisted.getTicker().getId());
        assertEquals(snapshot.getId(), persisted.getIndicatorSnapshot().getId());
        assertEquals(OrderStatus.FILLED, persisted.getStatus());

        // Closes the loop: the mock adapter's own state, not just the immediate placeOrder
        // return value, reflects the fill.
        Optional<BrokerPosition> position = brokerAdapter.getPosition(SYMBOL, TradingMode.PAPER);
        assertTrue(position.isPresent());
        assertEquals(0, request.quantity().compareTo(position.get().quantity()));
    }
}

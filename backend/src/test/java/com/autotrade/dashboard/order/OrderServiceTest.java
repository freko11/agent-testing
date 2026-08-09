package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.brokeradapter.BrokerAdapter;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterAmbiguousOrderException;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterException;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterRateLimitedException;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterRouter;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterUnavailableException;
import com.autotrade.dashboard.brokeradapter.BrokerOrderRequest;
import com.autotrade.dashboard.brokeradapter.BrokerOrderResult;
import com.autotrade.dashboard.common.PagedResponse;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.indicator.IndicatorResponse;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.marketdata.TickerSummary;
import com.autotrade.dashboard.notification.NotificationService;
import com.autotrade.dashboard.risk.KillSwitchCancelSummary;
import com.autotrade.dashboard.risk.KillSwitchEngagedException;
import com.autotrade.dashboard.risk.KillSwitchService;
import com.autotrade.dashboard.risk.RiskLimitExceededException;
import com.autotrade.dashboard.risk.RiskLimitService;
import com.autotrade.dashboard.risk.RiskLimitsProperties;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalCallEntryRepository;
import com.autotrade.dashboard.signal.SignalResponse;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.signal.SignalService;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.tradingmode.TradingModeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Proves signal recomputation, server-side re-validation, quantity conversion, adapter routing, and every placeOrder outcome map onto the persisted Order correctly. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private SignalService signalService;
    @Mock
    private BrokerAdapterRouter router;
    @Mock
    private BrokerCredentialService brokerCredentialService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private SignalCallEntryRepository signalCallEntryRepository;
    @Mock
    private OrderAuditEntryRepository orderAuditEntryRepository;
    @Mock
    private BrokerAdapter adapter;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TradingModeService tradingModeService;
    @Mock
    private RiskLimitService riskLimitService;
    @Mock
    private KillSwitchService killSwitchService;

    private OrderService service;
    private final AtomicReference<Order> saved = new AtomicReference<>();

    /** Mirrors the conservative starter defaults in application.properties (E6-F2-S1). */
    private static final RiskLimitsProperties REAL_RISK_LIMITS =
            new RiskLimitsProperties(new BigDecimal("5000"), new BigDecimal("5"), new BigDecimal("2000"), new BigDecimal("8000"));

    @BeforeEach
    void setUp() {
        service = new OrderService(signalService, router, brokerCredentialService, orderRepository,
                signalCallEntryRepository, orderAuditEntryRepository, notificationService, tradingModeService,
                riskLimitService, killSwitchService);
        lenient().when(tradingModeService.current()).thenReturn(TradingMode.PAPER);
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                ReflectionTestUtils.setField(order, "id", 1L);
            }
            saved.set(order);
            return order;
        });
        lenient().when(orderRepository.findById(1L)).thenAnswer(invocation -> Optional.ofNullable(saved.get()));
        lenient().when(orderRepository.sumOpenNotionalUsd(any(), any())).thenReturn(BigDecimal.ZERO);
        // Default stub: most tests only care about Order/TradeOrderResponse outcomes, not the audit row itself
        // (that's covered by dedicated tests below) -- any() here matches the null snapshot id most test fixtures
        // construct IndicatorSnapshot with, same as this stub not caring which SignalCallEntry it hands back.
        lenient().when(signalCallEntryRepository.findTopByIndicatorSnapshot_IdOrderByIdDesc(any()))
                .thenReturn(Optional.of(mock(SignalCallEntry.class)));
    }

    private Ticker cryptoTicker() {
        return new Ticker("BTCUSDT", AssetType.CRYPTO, null);
    }

    private SignalService.SignalComputation buyComputation(Ticker ticker, BigDecimal price) {
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-07-29T00:00:00Z"), price, Broker.BINANCE);
        MacdResult macd = new MacdResult(new BigDecimal("2.0"), new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal("0"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("111.0"), 30,
                new BigDecimal("108.0"), MovingAverageRelation.SHORT_ABOVE_LONG);
        IndicatorResponse indicators = new IndicatorResponse(TickerSummary.from(ticker), Broker.BINANCE,
                Instant.parse("2026-07-29T00:00:00Z"), price, new BigDecimal("25"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));
        SignalResponse response = new SignalResponse(TickerSummary.from(ticker), SignalCall.BUY, "BULLISH_MAJORITY",
                "rationale", "v1", null, indicators);
        return new SignalService.SignalComputation(response, snapshot);
    }

    private SignalService.SignalComputation holdComputation(Ticker ticker, BigDecimal price) {
        SignalService.SignalComputation buy = buyComputation(ticker, price);
        SignalResponse hold = new SignalResponse(buy.response().ticker(), SignalCall.HOLD, "CONFLICTING_SIGNALS",
                "rationale", "v1", null, buy.response().indicators());
        return new SignalService.SignalComputation(hold, buy.snapshot());
    }

    private BrokerCredential credential() {
        return new BrokerCredential(Broker.BINANCE, TradingMode.PAPER, "enc-key", "enc-secret");
    }

    private Order persistedOrder(Long id, Ticker ticker, String clientOrderId) {
        Order order = new Order(ticker, credential(), Broker.BINANCE, ticker.getAssetType(), OrderSide.BUY,
                new BigDecimal("1.00000000"), new BigDecimal("110"), new BigDecimal("90"), clientOrderId);
        order.setOrderMode(TradingMode.PAPER);
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    @Test
    void holdSignal_throwsSignalNotActionable_noOrderCreated() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(holdComputation(ticker, new BigDecimal("100")));

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("100"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        assertThrows(SignalNotActionableException.class, () -> service.submitOrder("BTCUSDT", request));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderAuditEntryRepository);
    }

    @Test
    void invalidLeverageForCrypto_throwsInvalidTradeRequest_noOrderCreated() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("100"), new BigDecimal("25"),
                new BigDecimal("110"), new BigDecimal("90"));

        assertThrows(InvalidTradeRequestException.class, () -> service.submitOrder("BTCUSDT", request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void takeProfitOnWrongSideForBuy_throwsInvalidTradeRequest() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("100"), BigDecimal.ONE,
                new BigDecimal("90"), new BigDecimal("80"));

        assertThrows(InvalidTradeRequestException.class, () -> service.submitOrder("BTCUSDT", request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void noBrokerCredentialConfigured_throwsBeforeOrderCreated() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.empty());

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("100"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        assertThrows(BrokerCredentialNotConfiguredException.class, () -> service.submitOrder("BTCUSDT", request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void killSwitchEngaged_blocksSubmitOrder_zeroSideEffects() {
        doThrow(new KillSwitchEngagedException()).when(killSwitchService).assertNotEngaged();

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("100"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        assertThrows(KillSwitchEngagedException.class, () -> service.submitOrder("BTCUSDT", request));
        verifyNoInteractions(signalService, orderRepository, adapter);
    }

    @Test
    void filledOrder_persistsAsFilledWithEntryPriceAndBrokerOrderId() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(credential()));

        BrokerOrderResult result = new BrokerOrderResult("client-id", "broker-order-1", OrderStatus.FILLED,
                new BigDecimal("100.5"), null, Instant.parse("2026-07-29T00:00:01Z"));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.PAPER))).thenReturn(result);

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("1000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        TradeOrderResponse response = service.submitOrder("BTCUSDT", request);

        assertEquals(OrderStatus.FILLED, response.status());
        assertEquals("broker-order-1", response.brokerOrderId());
        assertEquals(new BigDecimal("100.5"), response.filledPrice());
        assertEquals(new BigDecimal("10.00000000"), response.quantity());
        assertNull(response.rejectionReason());

        ArgumentCaptor<BrokerOrderRequest> requestCaptor = ArgumentCaptor.forClass(BrokerOrderRequest.class);
        verify(adapter).placeOrder(requestCaptor.capture(), eq(TradingMode.PAPER));
        assertEquals(OrderSide.BUY, requestCaptor.getValue().side());
        assertEquals(new BigDecimal("10.00000000"), requestCaptor.getValue().quantity());

        verify(notificationService).recordOrderOutcome(any(Order.class), eq(OrderStatus.PENDING));
    }

    /** E6-F3-S1: the immutable audit row is written exactly once, at the order's first resolved outcome, FK'd to
     * both the persisted {@code Order} and the {@code SignalCallEntry} already saved for this signal computation. */
    @Test
    void filledOrder_recordsAuditEntryLinkedToOrderAndSignalCall() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(credential()));
        SignalCallEntry signalCallEntry = mock(SignalCallEntry.class);
        when(signalCallEntry.getRuleTableVersion()).thenReturn(SignalRuleEngine.RULE_TABLE_VERSION);
        when(signalCallEntryRepository.findTopByIndicatorSnapshot_IdOrderByIdDesc(any()))
                .thenReturn(Optional.of(signalCallEntry));

        BrokerOrderResult result = new BrokerOrderResult("client-id", "broker-order-1", OrderStatus.FILLED,
                new BigDecimal("100.5"), null, Instant.parse("2026-07-29T00:00:01Z"));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.PAPER))).thenReturn(result);

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("1000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        service.submitOrder("BTCUSDT", request);

        ArgumentCaptor<OrderAuditEntry> auditCaptor = ArgumentCaptor.forClass(OrderAuditEntry.class);
        verify(orderAuditEntryRepository).save(auditCaptor.capture());
        OrderAuditEntry entry = auditCaptor.getValue();
        assertEquals(saved.get(), entry.getOrder());
        assertEquals(signalCallEntry, entry.getSignalCallEntry());
        assertEquals(SignalRuleEngine.RULE_TABLE_VERSION, entry.getRuleTableVersion());
        assertEquals(OrderStatus.FILLED, entry.getResultStatus());
        assertEquals("broker-order-1", entry.getBrokerOrderId());
        assertEquals(new BigDecimal("100.5"), entry.getEntryPrice());
        assertNull(entry.getRejectionReason());
    }

    @Test
    void rejectedOrder_persistsAsRejectedWithReason() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(credential()));

        BrokerOrderResult result = new BrokerOrderResult("client-id", null, OrderStatus.REJECTED,
                null, "Insufficient margin", Instant.parse("2026-07-29T00:00:01Z"));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.PAPER))).thenReturn(result);

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("1000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        TradeOrderResponse response = service.submitOrder("BTCUSDT", request);

        assertEquals(OrderStatus.REJECTED, response.status());
        assertEquals("Insufficient margin", response.rejectionReason());

        ArgumentCaptor<OrderAuditEntry> auditCaptor = ArgumentCaptor.forClass(OrderAuditEntry.class);
        verify(orderAuditEntryRepository).save(auditCaptor.capture());
        assertEquals(OrderStatus.REJECTED, auditCaptor.getValue().getResultStatus());
        assertEquals("Insufficient margin", auditCaptor.getValue().getRejectionReason());
    }

    @Test
    void ambiguousOutcome_persistsAsSubmissionUnknown() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(credential()));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.PAPER)))
                .thenThrow(new BrokerAdapterAmbiguousOrderException(Broker.BINANCE, "client-id", new RuntimeException("boom")));

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("1000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        TradeOrderResponse response = service.submitOrder("BTCUSDT", request);

        assertEquals(OrderStatus.SUBMISSION_UNKNOWN, response.status());
    }

    @Test
    void unavailableOutcome_persistsAsFailed() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(credential()));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.PAPER)))
                .thenThrow(new BrokerAdapterUnavailableException(Broker.BINANCE, new RuntimeException("down")));

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("1000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        TradeOrderResponse response = service.submitOrder("BTCUSDT", request);

        assertEquals(OrderStatus.FAILED, response.status());
    }

    @Test
    void rateLimitedOutcome_persistsAsFailed() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(credential()));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.PAPER)))
                .thenThrow(new BrokerAdapterRateLimitedException(Broker.BINANCE, 30L));

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("1000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        TradeOrderResponse response = service.submitOrder("BTCUSDT", request);

        assertEquals(OrderStatus.FAILED, response.status());
    }

    @Test
    void fatalBrokerException_persistsAsFailed() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(credential()));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.PAPER)))
                .thenThrow(new BrokerAdapterException(Broker.BINANCE, "malformed request"));

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("1000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        TradeOrderResponse response = service.submitOrder("BTCUSDT", request);

        assertEquals(OrderStatus.FAILED, response.status());
        assertEquals("malformed request", response.rejectionReason());
    }

    @Test
    void sellSignal_stockAssetType_leverageForcedTo1x() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-07-29T00:00:00Z"),
                new BigDecimal("200"), Broker.ALPACA);
        MacdResult macd = new MacdResult(new BigDecimal("-2.0"), new BigDecimal("-1.0"), new BigDecimal("-1.0"), new BigDecimal("0"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("108.0"), 30,
                new BigDecimal("111.0"), MovingAverageRelation.SHORT_BELOW_LONG);
        IndicatorResponse indicators = new IndicatorResponse(TickerSummary.from(ticker), Broker.ALPACA,
                Instant.parse("2026-07-29T00:00:00Z"), new BigDecimal("200"), new BigDecimal("75"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));
        SignalResponse signalResponse = new SignalResponse(TickerSummary.from(ticker), SignalCall.SELL,
                "BEARISH_MAJORITY", "rationale", "v1", null, indicators);
        when(signalService.computeSignalWithProvenance("AAPL", 200))
                .thenReturn(new SignalService.SignalComputation(signalResponse, snapshot));

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("2000"), new BigDecimal("5"),
                new BigDecimal("190"), new BigDecimal("210"));

        assertThrows(InvalidTradeRequestException.class, () -> service.submitOrder("AAPL", request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void submitOrder_usesCurrentGlobalTradingMode_notHardcodedPaper() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(tradingModeService.current()).thenReturn(TradingMode.LIVE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.LIVE)).thenReturn(Optional.of(credential()));

        BrokerOrderResult result = new BrokerOrderResult("client-id", "broker-order-1", OrderStatus.FILLED,
                new BigDecimal("100.5"), null, Instant.parse("2026-07-29T00:00:01Z"));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.LIVE))).thenReturn(result);

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("1000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        service.submitOrder("BTCUSDT", request);

        verify(brokerCredentialService).find(Broker.BINANCE, TradingMode.LIVE);
        verify(adapter).placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.LIVE));
        assertEquals(TradingMode.LIVE, saved.get().getOrderMode());
    }

    /** Uses a real {@link RiskLimitService} (not the class's mocked {@code riskLimitService} field) so these E6-F2-S1
     * tests exercise genuine cap enforcement end to end, not just that {@code OrderService} calls its collaborator. */
    private OrderService serviceWithRealRiskLimits() {
        return new OrderService(signalService, router, brokerCredentialService, orderRepository,
                signalCallEntryRepository, orderAuditEntryRepository, notificationService, tradingModeService,
                new RiskLimitService(REAL_RISK_LIMITS), killSwitchService);
    }

    @Test
    void overCapLeverage_forgedPlaceOrderRequest_rejectedNoOrderRowNoAdapterCall() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        OrderService serviceWithRealCaps = serviceWithRealRiskLimits();

        // 10x is within OrderService.MAX_CRYPTO_LEVERAGE (20x) and within adapter-technical bounds, but above the
        // configured 5x risk cap — a request never touched by frontend/src/trade/validation.ts, simulating a
        // forged/direct API call that a UI bug or fat-finger could never reach through the form.
        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("100"), new BigDecimal("10"),
                new BigDecimal("110"), new BigDecimal("90"));

        assertThrows(RiskLimitExceededException.class, () -> serviceWithRealCaps.submitOrder("BTCUSDT", request));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(adapter);
        verifyNoInteractions(orderAuditEntryRepository);
    }

    @Test
    void overCapPositionSize_forgedPlaceOrderRequest_rejectedNoOrderRowNoAdapterCall() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        OrderService serviceWithRealCaps = serviceWithRealRiskLimits();

        // Leverage (1x) is well within cap, but notional = amountUsd * leverage = 3000 exceeds the 2000 position-size cap.
        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("3000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        assertThrows(RiskLimitExceededException.class, () -> serviceWithRealCaps.submitOrder("BTCUSDT", request));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(adapter);
    }

    @Test
    void atExactRiskCap_allowed_orderSubmittedNormally() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(credential()));
        BrokerOrderResult result = new BrokerOrderResult("client-id", "broker-order-1", OrderStatus.FILLED,
                new BigDecimal("100.5"), null, Instant.parse("2026-07-29T00:00:01Z"));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.PAPER))).thenReturn(result);
        OrderService serviceWithRealCaps = serviceWithRealRiskLimits();

        // notional = 2000 * 1 = 2000, exactly at the configured crypto position-size cap — boundary-inclusive.
        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("2000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        TradeOrderResponse response = serviceWithRealCaps.submitOrder("BTCUSDT", request);

        assertEquals(OrderStatus.FILLED, response.status());
    }

    @Test
    void overAggregateExposureCap_manySmallOrdersAddingUp_rejectedNoOrderRowNoAdapterCall() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        // 6500 already open in PAPER mode (well within any single per-order cap) + this 2000 order = 8500 > the
        // 8000 aggregate cap — proves the cap catches many individually-small orders adding up, not just one big one.
        when(orderRepository.sumOpenNotionalUsd(eq(TradingMode.PAPER), any())).thenReturn(new BigDecimal("6500"));
        OrderService serviceWithRealCaps = serviceWithRealRiskLimits();

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("2000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        assertThrows(RiskLimitExceededException.class, () -> serviceWithRealCaps.submitOrder("BTCUSDT", request));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(adapter);
    }

    @Test
    void atExactAggregateExposureCap_allowed_orderSubmittedNormally() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(buyComputation(ticker, new BigDecimal("100")));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.broker()).thenReturn(Broker.BINANCE);
        when(brokerCredentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(credential()));
        BrokerOrderResult result = new BrokerOrderResult("client-id", "broker-order-1", OrderStatus.FILLED,
                new BigDecimal("100.5"), null, Instant.parse("2026-07-29T00:00:01Z"));
        when(adapter.placeOrder(any(BrokerOrderRequest.class), eq(TradingMode.PAPER))).thenReturn(result);
        // 6000 already open + this 2000 order = exactly 8000, the configured aggregate cap — boundary-inclusive.
        when(orderRepository.sumOpenNotionalUsd(any(), any())).thenReturn(new BigDecimal("6000"));
        OrderService serviceWithRealCaps = serviceWithRealRiskLimits();

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("2000"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        TradeOrderResponse response = serviceWithRealCaps.submitOrder("BTCUSDT", request);

        assertEquals(OrderStatus.FILLED, response.status());
    }

    @Test
    void cancelAllOpenOrders_nonTerminalAcrossBothBrokers_cancelsEachThroughItsAdapter() {
        Ticker cryptoTicker = cryptoTicker();
        Order cryptoOrder = persistedOrder(20L, cryptoTicker, "client-20");
        cryptoOrder.setStatus(OrderStatus.SUBMITTED);

        Ticker stockTicker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        Order stockOrder = new Order(stockTicker, credential(), Broker.ALPACA, AssetType.STOCK, OrderSide.BUY,
                new BigDecimal("1"), new BigDecimal("210"), new BigDecimal("190"), "client-21");
        stockOrder.setOrderMode(TradingMode.PAPER);
        stockOrder.setStatus(OrderStatus.PARTIALLY_PROTECTED);
        ReflectionTestUtils.setField(stockOrder, "id", 21L);

        when(orderRepository.findByStatusNotIn(any())).thenReturn(List.of(cryptoOrder, stockOrder));
        when(orderRepository.findById(20L)).thenReturn(Optional.of(cryptoOrder));
        when(orderRepository.findById(21L)).thenReturn(Optional.of(stockOrder));

        BrokerAdapter alpacaAdapter = mock(BrokerAdapter.class);
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(router.forAssetType(AssetType.STOCK)).thenReturn(alpacaAdapter);

        when(adapter.cancelOrder("BTCUSDT", "client-20", TradingMode.PAPER)).thenReturn(
                new BrokerOrderResult("client-20", "broker-20", OrderStatus.CANCELLED, null, null,
                        Instant.parse("2026-07-29T00:00:03Z")));
        when(alpacaAdapter.cancelOrder("AAPL", "client-21", TradingMode.PAPER)).thenReturn(
                new BrokerOrderResult("client-21", "broker-21", OrderStatus.CANCELLED, null, null,
                        Instant.parse("2026-07-29T00:00:03Z")));

        KillSwitchCancelSummary summary = service.cancelAllOpenOrders();

        assertEquals(2, summary.attempted());
        assertEquals(2, summary.cancelled());
        assertEquals(0, summary.failed());
        assertEquals(OrderStatus.CANCELLED, cryptoOrder.getStatus());
        assertEquals(OrderStatus.CANCELLED, stockOrder.getStatus());
    }

    @Test
    void cancelAllOpenOrders_oneOrderFails_othersStillCancelled_failureRecordedNotStatusOverwritten() {
        Ticker ticker = cryptoTicker();
        Order failingOrder = persistedOrder(30L, ticker, "client-30");
        failingOrder.setStatus(OrderStatus.SUBMITTED);
        Order succeedingOrder = persistedOrder(31L, ticker, "client-31");
        succeedingOrder.setStatus(OrderStatus.SUBMITTED);

        when(orderRepository.findByStatusNotIn(any())).thenReturn(List.of(failingOrder, succeedingOrder));
        when(orderRepository.findById(31L)).thenReturn(Optional.of(succeedingOrder));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.cancelOrder("BTCUSDT", "client-30", TradingMode.PAPER))
                .thenThrow(new BrokerAdapterException(Broker.BINANCE, "outage"));
        when(adapter.cancelOrder("BTCUSDT", "client-31", TradingMode.PAPER)).thenReturn(
                new BrokerOrderResult("client-31", "broker-31", OrderStatus.CANCELLED, null, null,
                        Instant.parse("2026-07-29T00:00:03Z")));

        KillSwitchCancelSummary summary = service.cancelAllOpenOrders();

        assertEquals(2, summary.attempted());
        assertEquals(1, summary.cancelled());
        assertEquals(1, summary.failed());
        assertTrue(summary.failureMessages().get(0).contains("client-30"));
        assertEquals(OrderStatus.SUBMITTED, failingOrder.getStatus());
        assertEquals(OrderStatus.CANCELLED, succeedingOrder.getStatus());
    }

    @Test
    void listOrders_mapsRepositoryResultsToResponses() {
        Ticker ticker = cryptoTicker();
        Order order = persistedOrder(5L, ticker, "client-1");
        order.setStatus(OrderStatus.FILLED);
        when(orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50))).thenReturn(List.of(order));

        List<OrderResponse> responses = service.listOrders(50);

        assertEquals(1, responses.size());
        assertEquals(5L, responses.get(0).id());
        assertEquals("BTCUSDT", responses.get(0).tickerSymbol());
        assertEquals(OrderStatus.FILLED, responses.get(0).status());
    }

    @Test
    void listAuditEntries_mapsRepositoryPageToPagedResponseOfAuditEntryResponse() {
        Ticker ticker = cryptoTicker();
        Order order = persistedOrder(5L, ticker, "client-1");
        SignalCallEntry signalCallEntry = mock(SignalCallEntry.class);
        when(signalCallEntry.getCall()).thenReturn(SignalCall.BUY);
        when(signalCallEntry.getMatchedRule()).thenReturn(SignalRuleId.BULLISH_MAJORITY);
        when(signalCallEntry.getHoldTermMinDays()).thenReturn(1);
        when(signalCallEntry.getHoldTermMaxDays()).thenReturn(5);
        OrderAuditEntry entry = new OrderAuditEntry(order, signalCallEntry, "v2", OrderStatus.FILLED, null,
                "broker-order-1", new BigDecimal("100.5"));
        ReflectionTestUtils.setField(entry, "id", 9L);
        when(orderAuditEntryRepository.findAllByOrderByLoggedAtDesc(PageRequest.of(0, 25)))
                .thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(0, 25), 1));

        PagedResponse<AuditEntryResponse> response = service.listAuditEntries(0, 25);

        assertEquals(1, response.totalElements());
        assertEquals(1, response.content().size());
        AuditEntryResponse mapped = response.content().get(0);
        assertEquals(9L, mapped.id());
        assertEquals("BTCUSDT", mapped.tickerSymbol());
        assertEquals(OrderSide.BUY, mapped.side());
        assertEquals(SignalCall.BUY, mapped.call());
        assertEquals(SignalRuleId.BULLISH_MAJORITY, mapped.matchedRule());
        assertEquals(SignalRuleId.BULLISH_MAJORITY.rationale(), mapped.matchedRuleRationale());
        assertEquals("v2", mapped.ruleTableVersion());
        assertEquals(OrderStatus.FILLED, mapped.resultStatus());
    }

    @Test
    void refreshOrder_unknownId_throwsOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.refreshOrder(99L));
    }

    @Test
    void refreshOrder_brokerConfirmsFilled_persistsUpdatedStatus() {
        Ticker ticker = cryptoTicker();
        Order order = persistedOrder(7L, ticker, "client-7");
        order.setStatus(OrderStatus.SUBMISSION_UNKNOWN);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        BrokerOrderResult result = new BrokerOrderResult("client-7", "broker-7", OrderStatus.FILLED,
                new BigDecimal("101.0"), null, Instant.parse("2026-07-29T00:00:02Z"));
        when(adapter.getOrderStatus("BTCUSDT", "client-7", TradingMode.PAPER)).thenReturn(Optional.of(result));

        OrderResponse response = service.refreshOrder(7L);

        assertEquals(OrderStatus.FILLED, response.status());
        assertEquals("broker-7", response.brokerOrderId());
        assertEquals(new BigDecimal("101.0"), response.entryPrice());
        verify(notificationService).recordOrderOutcome(any(Order.class), eq(OrderStatus.SUBMISSION_UNKNOWN));
        // E6-F3-S1 scope boundary: refreshOrder resolving a status further never touches the audit log --
        // that row was already frozen at submitOrder time and stays frozen.
        verifyNoInteractions(orderAuditEntryRepository);
    }

    @Test
    void refreshOrder_brokerConfirmsSameStatus_doesNotNotify() {
        Ticker ticker = cryptoTicker();
        Order order = persistedOrder(12L, ticker, "client-12");
        order.setStatus(OrderStatus.FILLED);
        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        BrokerOrderResult result = new BrokerOrderResult("client-12", "broker-12", OrderStatus.FILLED,
                new BigDecimal("101.0"), null, Instant.parse("2026-07-29T00:00:02Z"));
        when(adapter.getOrderStatus("BTCUSDT", "client-12", TradingMode.PAPER)).thenReturn(Optional.of(result));

        OrderResponse response = service.refreshOrder(12L);

        assertEquals(OrderStatus.FILLED, response.status());
        verify(notificationService, never()).recordOrderOutcome(any(), any());
    }

    @Test
    void refreshOrder_brokerHasNoRecord_persistsFailedSafeToRetry() {
        Ticker ticker = cryptoTicker();
        Order order = persistedOrder(8L, ticker, "client-8");
        order.setStatus(OrderStatus.SUBMISSION_UNKNOWN);
        when(orderRepository.findById(8L)).thenReturn(Optional.of(order));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.getOrderStatus("BTCUSDT", "client-8", TradingMode.PAPER)).thenReturn(Optional.empty());

        OrderResponse response = service.refreshOrder(8L);

        assertEquals(OrderStatus.FAILED, response.status());
        assertEquals("Broker confirmed no record of this order — safe to retry.", response.rejectionReason());
    }

    @Test
    void refreshOrder_ignoresCurrentGlobalTradingMode_usesOrdersOwnPersistedMode() {
        Ticker ticker = cryptoTicker();
        Order order = persistedOrder(13L, ticker, "client-13");
        order.setStatus(OrderStatus.SUBMISSION_UNKNOWN);
        when(orderRepository.findById(13L)).thenReturn(Optional.of(order));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        // The global switch has since moved on to LIVE, but this order was placed under PAPER —
        // refreshOrder must poll the broker environment the order actually lives in, not "whatever mode is
        // active now". This stub is deliberately never expected to be consulted (lenient, not strict) —
        // that's the whole point of this regression guard.
        lenient().when(tradingModeService.current()).thenReturn(TradingMode.LIVE);
        BrokerOrderResult result = new BrokerOrderResult("client-13", "broker-13", OrderStatus.FILLED,
                new BigDecimal("101.0"), null, Instant.parse("2026-07-29T00:00:02Z"));
        when(adapter.getOrderStatus("BTCUSDT", "client-13", TradingMode.PAPER)).thenReturn(Optional.of(result));

        service.refreshOrder(13L);

        verify(adapter).getOrderStatus("BTCUSDT", "client-13", TradingMode.PAPER);
    }

    @Test
    void refreshOrder_brokerThrows_leavesStoredRowUntouched() {
        Ticker ticker = cryptoTicker();
        Order order = persistedOrder(9L, ticker, "client-9");
        order.setStatus(OrderStatus.PARTIALLY_PROTECTED);
        order.setRejectionReason("Missing stop-loss leg");
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));
        when(router.forAssetType(AssetType.CRYPTO)).thenReturn(adapter);
        when(adapter.getOrderStatus("BTCUSDT", "client-9", TradingMode.PAPER))
                .thenThrow(new BrokerAdapterException(Broker.BINANCE, "rate limited"));

        assertThrows(OrderRefreshUnavailableException.class, () -> service.refreshOrder(9L));

        assertEquals(OrderStatus.PARTIALLY_PROTECTED, order.getStatus());
        assertEquals("Missing stop-loss leg", order.getRejectionReason());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void exportOrdersCsv_startAfterEnd_throwsInvalidTradeRequest() {
        LocalDate start = LocalDate.parse("2026-07-10");
        LocalDate end = LocalDate.parse("2026-07-01");

        assertThrows(InvalidTradeRequestException.class, () -> service.exportOrdersCsv(start, end, null));
    }

    @Test
    void exportOrdersCsv_noMode_usesModeAgnosticQueryAndIncludesSignalReference() {
        Ticker ticker = cryptoTicker();
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-07-05T00:00:00Z"),
                new BigDecimal("100"), Broker.BINANCE);
        ReflectionTestUtils.setField(snapshot, "id", 42L);
        Order order = persistedOrder(11L, ticker, "client-11");
        order.setIndicatorSnapshot(snapshot);
        SignalCallEntry signalCallEntry = new SignalCallEntry(ticker, snapshot, SignalRuleId.BULLISH_MAJORITY, null);

        LocalDate start = LocalDate.parse("2026-07-01");
        LocalDate end = LocalDate.parse("2026-07-10");
        when(orderRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(order));
        when(signalCallEntryRepository.findByIndicatorSnapshot_IdIn(List.of(42L))).thenReturn(List.of(signalCallEntry));

        String csv = service.exportOrdersCsv(start, end, null);

        assertTrue(csv.contains("Order ID,Created At (UTC)"));
        assertTrue(csv.contains("client-11"));
        assertTrue(csv.contains("BULLISH_MAJORITY"));
        verify(orderRepository, never())
                .findByOrderModeAndCreatedAtBetweenOrderByCreatedAtAsc(any(), any(), any());
    }

    @Test
    void exportOrdersCsv_withMode_usesModeFilteredQuery() {
        LocalDate start = LocalDate.parse("2026-07-01");
        LocalDate end = LocalDate.parse("2026-07-10");
        when(orderRepository.findByOrderModeAndCreatedAtBetweenOrderByCreatedAtAsc(
                eq(TradingMode.PAPER), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        String csv = service.exportOrdersCsv(start, end, TradingMode.PAPER);

        assertTrue(csv.startsWith("Order ID,Created At (UTC)"));
        verify(orderRepository, never()).findByCreatedAtBetweenOrderByCreatedAtAsc(any(), any());
    }
}

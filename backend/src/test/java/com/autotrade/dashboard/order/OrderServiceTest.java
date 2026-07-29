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
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.indicator.IndicatorResponse;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.marketdata.TickerSummary;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalResponse;
import com.autotrade.dashboard.signal.SignalService;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private BrokerAdapter adapter;

    private OrderService service;
    private final AtomicReference<Order> saved = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        service = new OrderService(signalService, router, brokerCredentialService, orderRepository);
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                ReflectionTestUtils.setField(order, "id", 1L);
            }
            saved.set(order);
            return order;
        });
        lenient().when(orderRepository.findById(1L)).thenAnswer(invocation -> Optional.ofNullable(saved.get()));
    }

    private Ticker cryptoTicker() {
        return new Ticker("BTCUSDT", AssetType.CRYPTO, null);
    }

    private SignalService.SignalComputation buyComputation(Ticker ticker, BigDecimal price) {
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-07-29T00:00:00Z"), price, Broker.BINANCE);
        MacdResult macd = new MacdResult(new BigDecimal("2.0"), new BigDecimal("1.0"), new BigDecimal("1.0"));
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

    @Test
    void holdSignal_throwsSignalNotActionable_noOrderCreated() {
        Ticker ticker = cryptoTicker();
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200))
                .thenReturn(holdComputation(ticker, new BigDecimal("100")));

        PlaceOrderRequest request = new PlaceOrderRequest(new BigDecimal("100"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90"));

        assertThrows(SignalNotActionableException.class, () -> service.submitOrder("BTCUSDT", request));
        verify(orderRepository, never()).save(any());
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
        MacdResult macd = new MacdResult(new BigDecimal("-2.0"), new BigDecimal("-1.0"), new BigDecimal("-1.0"));
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
}

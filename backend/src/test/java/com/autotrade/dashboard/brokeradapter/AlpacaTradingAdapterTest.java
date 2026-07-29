package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Proves JSON<->DTO mapping, auth-header wiring, and error classification for
 * E4-F2-S1's real Alpaca trading adapter — no live HTTP, no Spring context
 * (same posture as {@code AlpacaMarketDataClientTest}). Full-interface
 * plumbing correctness (idempotent replay, empty-Optional cases, etc.) is
 * covered separately by {@link AlpacaTradingAdapterContractTest}.
 */
@ExtendWith(MockitoExtension.class)
class AlpacaTradingAdapterTest {

    private static final String BASE_URL = "https://paper-api.alpaca.markets";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-29T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private BrokerCredentialService credentialService;

    private MockRestServiceServer server;
    private AlpacaTradingAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new AlpacaTradingAdapter(Map.of(TradingMode.PAPER, builder.build()), credentialService, CLOCK);

        BrokerCredential stored = new BrokerCredential(Broker.ALPACA, TradingMode.PAPER, "ignored-ciphertext", "ignored-ciphertext");
        lenient().when(credentialService.find(Broker.ALPACA, TradingMode.PAPER)).thenReturn(Optional.of(stored));
        lenient().when(credentialService.readDecrypted(stored))
                .thenReturn(new BrokerCredentialService.DecryptedCredential("test-key", "test-secret"));
    }

    private BrokerOrderRequest sampleBuyRequest(String clientOrderId) {
        return new BrokerOrderRequest(
                clientOrderId, "AAPL", AssetType.STOCK, OrderSide.BUY, new BigDecimal("10"),
                EntryOrderType.MARKET, null, new BigDecimal("220"), new BigDecimal("180"), BigDecimal.ONE);
    }

    @Test
    void placeOrder_success_sendsBracketBodyAndAuthHeaders() {
        server.expect(requestTo(BASE_URL + "/v2/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("APCA-API-KEY-ID", "test-key"))
                .andExpect(header("APCA-API-SECRET-KEY", "test-secret"))
                .andExpect(content().json(
                        "{\"symbol\":\"AAPL\",\"qty\":\"10\",\"side\":\"buy\",\"type\":\"market\","
                                + "\"time_in_force\":\"day\",\"order_class\":\"bracket\",\"client_order_id\":\"co-1\","
                                + "\"take_profit\":{\"limit_price\":\"220\"},\"stop_loss\":{\"stop_price\":\"180\"}}"))
                .andRespond(withSuccess(
                        "{\"id\":\"alpaca-order-1\",\"client_order_id\":\"co-1\",\"status\":\"new\",\"filled_avg_price\":null}",
                        MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(sampleBuyRequest("co-1"), TradingMode.PAPER);

        assertEquals("co-1", result.clientOrderId());
        assertEquals("alpaca-order-1", result.brokerOrderId());
        assertEquals(OrderStatus.SUBMITTED, result.status());
        assertNull(result.filledPrice());
        server.verify();
    }

    @Test
    void placeOrder_filledResponse_mapsFilledPrice() {
        server.expect(requestTo(BASE_URL + "/v2/orders"))
                .andRespond(withSuccess(
                        "{\"id\":\"alpaca-order-1\",\"client_order_id\":\"co-1\",\"status\":\"filled\",\"filled_avg_price\":\"200.50\"}",
                        MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(sampleBuyRequest("co-1"), TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(0, new BigDecimal("200.50").compareTo(result.filledPrice()));
    }

    @Test
    void placeOrder_limitOrder_includesLimitPrice() {
        BrokerOrderRequest limitRequest = new BrokerOrderRequest(
                "co-limit", "AAPL", AssetType.STOCK, OrderSide.BUY, new BigDecimal("5"),
                EntryOrderType.LIMIT, new BigDecimal("199.5"), new BigDecimal("220"), new BigDecimal("180"), BigDecimal.ONE);

        server.expect(requestTo(BASE_URL + "/v2/orders"))
                .andExpect(content().json(
                        "{\"type\":\"limit\",\"limit_price\":\"199.5\"}"))
                .andRespond(withSuccess(
                        "{\"id\":\"alpaca-order-2\",\"client_order_id\":\"co-limit\",\"status\":\"new\"}",
                        MediaType.APPLICATION_JSON));

        adapter.placeOrder(limitRequest, TradingMode.PAPER);
        server.verify();
    }

    @Test
    void placeOrder_forbidden_returnsRejectedResultRatherThanThrowing() {
        server.expect(requestTo(BASE_URL + "/v2/orders"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .body("{\"code\":40310000,\"message\":\"insufficient buying power\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(sampleBuyRequest("co-1"), TradingMode.PAPER);

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals("insufficient buying power", result.rejectionReason());
        assertNull(result.brokerOrderId());
    }

    @Test
    void placeOrder_duplicateClientOrderId_replaysExistingOrderViaGetOrderStatus() {
        server.expect(requestTo(BASE_URL + "/v2/orders"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("{\"code\":40010001,\"message\":\"client_order_id must be unique\"}")
                        .contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/v2/orders:by_client_order_id?client_order_id=co-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":\"alpaca-order-1\",\"client_order_id\":\"co-1\",\"status\":\"filled\",\"filled_avg_price\":\"201\"}",
                        MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(sampleBuyRequest("co-1"), TradingMode.PAPER);

        assertEquals("alpaca-order-1", result.brokerOrderId());
        assertEquals(OrderStatus.FILLED, result.status());
        server.verify();
    }

    @Test
    void placeOrder_malformedRequest_throwsFatalNonRetryableException() {
        server.expect(requestTo(BASE_URL + "/v2/orders"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("{\"code\":40010002,\"message\":\"qty must be positive\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class,
                () -> adapter.placeOrder(sampleBuyRequest("co-1"), TradingMode.PAPER));

        assertTrue(ex.getMessage().contains("qty must be positive"));
        assertEquals(BrokerAdapterException.class, ex.getClass());
    }

    @Test
    void placeOrder_rateLimited_throwsRateLimitedWithRetryAfter() {
        server.expect(requestTo(BASE_URL + "/v2/orders"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "15"));

        BrokerAdapterRateLimitedException ex = assertThrows(BrokerAdapterRateLimitedException.class,
                () -> adapter.placeOrder(sampleBuyRequest("co-1"), TradingMode.PAPER));

        assertEquals(15L, ex.retryAfterSeconds());
    }

    @Test
    void placeOrder_serverError_throwsTransientException() {
        server.expect(requestTo(BASE_URL + "/v2/orders")).andRespond(withServerError());

        assertThrows(BrokerAdapterTransientException.class,
                () -> adapter.placeOrder(sampleBuyRequest("co-1"), TradingMode.PAPER));
    }

    @Test
    void placeOrder_noCredentialConfigured_throwsWithoutCallingHttp() {
        when(credentialService.find(Broker.ALPACA, TradingMode.LIVE)).thenReturn(Optional.empty());

        assertThrows(BrokerAdapterException.class,
                () -> adapter.placeOrder(sampleBuyRequest("co-1"), TradingMode.LIVE));
        server.verify();
    }

    @Test
    void placeOrder_leverageOnStockOrder_throwsWithoutCallingHttp() {
        BrokerOrderRequest leveraged = new BrokerOrderRequest(
                "co-1", "AAPL", AssetType.STOCK, OrderSide.BUY, new BigDecimal("10"),
                EntryOrderType.MARKET, null, new BigDecimal("220"), new BigDecimal("180"), new BigDecimal("2"));

        assertThrows(BrokerAdapterException.class, () -> adapter.placeOrder(leveraged, TradingMode.PAPER));
        server.verify();
    }

    @Test
    void getOrderStatus_found_mapsResult() {
        server.expect(requestTo(BASE_URL + "/v2/orders:by_client_order_id?client_order_id=co-1"))
                .andRespond(withSuccess(
                        "{\"id\":\"alpaca-order-1\",\"client_order_id\":\"co-1\",\"status\":\"partially_filled\",\"filled_avg_price\":\"199.9\"}",
                        MediaType.APPLICATION_JSON));

        Optional<BrokerOrderResult> result = adapter.getOrderStatus("co-1", TradingMode.PAPER);

        assertTrue(result.isPresent());
        assertEquals(OrderStatus.PARTIALLY_FILLED, result.get().status());
    }

    @Test
    void getOrderStatus_notFound_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/v2/orders:by_client_order_id?client_order_id=missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(adapter.getOrderStatus("missing", TradingMode.PAPER).isEmpty());
    }

    @Test
    void getPosition_found_mapsResult() {
        server.expect(requestTo(BASE_URL + "/v2/positions/AAPL"))
                .andRespond(withSuccess(
                        "{\"qty\":\"10\",\"avg_entry_price\":\"200.5\",\"unrealized_pl\":\"15\"}",
                        MediaType.APPLICATION_JSON));

        Optional<BrokerPosition> position = adapter.getPosition("AAPL", TradingMode.PAPER);

        assertTrue(position.isPresent());
        assertEquals(0, new BigDecimal("10").compareTo(position.get().quantity()));
        assertEquals(AssetType.STOCK, position.get().assetType());
    }

    @Test
    void getPosition_notFound_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/v2/positions/ZZZZ")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(adapter.getPosition("ZZZZ", TradingMode.PAPER).isEmpty());
    }

    @Test
    void cancelOrder_unknownClientOrderId_returnsFailedWithoutThrowingOrCallingDelete() {
        server.expect(requestTo(BASE_URL + "/v2/orders:by_client_order_id?client_order_id=missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        BrokerOrderResult result = adapter.cancelOrder("missing", TradingMode.PAPER);

        assertEquals(OrderStatus.FAILED, result.status());
        assertEquals("Unknown clientOrderId", result.rejectionReason());
        server.verify();
    }

    @Test
    void cancelOrder_openOrder_deletesThenReturnsRefetchedStatus() {
        server.expect(requestTo(BASE_URL + "/v2/orders:by_client_order_id?client_order_id=co-1"))
                .andRespond(withSuccess(
                        "{\"id\":\"alpaca-order-1\",\"client_order_id\":\"co-1\",\"status\":\"new\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/v2/orders/alpaca-order-1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        server.expect(requestTo(BASE_URL + "/v2/orders:by_client_order_id?client_order_id=co-1"))
                .andRespond(withSuccess(
                        "{\"id\":\"alpaca-order-1\",\"client_order_id\":\"co-1\",\"status\":\"canceled\"}",
                        MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.cancelOrder("co-1", TradingMode.PAPER);

        assertEquals(OrderStatus.CANCELLED, result.status());
        server.verify();
    }

    @Test
    void cancelOrder_alreadyTerminalOrder_isIdempotentNoOpWithoutCallingDelete() {
        server.expect(requestTo(BASE_URL + "/v2/orders:by_client_order_id?client_order_id=co-1"))
                .andRespond(withSuccess(
                        "{\"id\":\"alpaca-order-1\",\"client_order_id\":\"co-1\",\"status\":\"filled\",\"filled_avg_price\":\"200\"}",
                        MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.cancelOrder("co-1", TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, result.status());
        server.verify();
    }

    @Test
    void getAccountStatus_mapsBalanceEquityAndBuyingPower() {
        server.expect(requestTo(BASE_URL + "/v2/account"))
                .andRespond(withSuccess(
                        "{\"cash\":\"98000.00\",\"equity\":\"100500.00\",\"buying_power\":\"196000.00\",\"currency\":\"USD\"}",
                        MediaType.APPLICATION_JSON));

        BrokerAccountStatus status = adapter.getAccountStatus(TradingMode.PAPER);

        assertEquals(1, status.balances().size());
        assertEquals("USD", status.balances().get(0).asset());
        assertEquals(0, new BigDecimal("98000.00").compareTo(status.balances().get(0).free()));
        assertEquals(0, new BigDecimal("100500.00").compareTo(status.equity()));
        assertEquals(0, new BigDecimal("196000.00").compareTo(status.buyingPower()));
    }

    @Test
    void getAccountStatus_rateLimited_throwsRateLimited() {
        server.expect(requestTo(BASE_URL + "/v2/account"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "5"));

        assertThrows(BrokerAdapterRateLimitedException.class, () -> adapter.getAccountStatus(TradingMode.PAPER));
    }
}

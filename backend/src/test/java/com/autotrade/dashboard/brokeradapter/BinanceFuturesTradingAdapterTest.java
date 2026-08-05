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

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Proves HMAC-SHA256 signing, JSON&lt;-&gt;DTO mapping, and error
 * classification for E4-F3-S1's real Binance Futures Testnet adapter — no
 * live HTTP, no Spring context (same posture as {@code
 * AlpacaTradingAdapterTest}). The exact expected signed query strings below
 * were computed independently (Python's {@code hmac}/{@code hashlib}, not
 * copied from adapter output) against the fixed clock/key/secret this test
 * uses, per this codebase's existing "compute reference values
 * independently" discipline (E2's indicator tests). Full-interface plumbing
 * correctness is covered separately by {@link
 * BinanceFuturesTradingAdapterContractTest}.
 */
@ExtendWith(MockitoExtension.class)
class BinanceFuturesTradingAdapterTest {

    private static final String BASE_URL = "https://testnet.binancefuture.com";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-29T12:00:00Z"), ZoneOffset.UTC);

    // Independently computed: hmac.new(b"test-secret", b"timestamp=1785326400000&recvWindow=5000", hashlib.sha256).hexdigest()
    private static final String ACCOUNT_SIGNATURE = "ff800b4f7ba5b39878cf4dd2bdea042f4356f1438edbb4a3b7749de8160c6583";
    // Independently computed: hmac.new(b"test-secret", b"symbol=BTCUSDT&timestamp=1785326400000&recvWindow=5000", hashlib.sha256).hexdigest()
    private static final String POSITION_SIGNATURE = "1d4183c1a74574cc40a48bccdf384fd2d6cc06767e3ffba858bde7ba30a42c84";

    @Mock
    private BrokerCredentialService credentialService;

    private MockRestServiceServer server;
    private BinanceFuturesTradingAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BinanceFuturesTradingAdapter(Map.of(TradingMode.PAPER, builder.build()), credentialService, CLOCK);

        BrokerCredential stored = new BrokerCredential(Broker.BINANCE, TradingMode.PAPER, "ignored-ciphertext", "ignored-ciphertext");
        lenient().when(credentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(stored));
        lenient().when(credentialService.readDecrypted(stored))
                .thenReturn(new BrokerCredentialService.DecryptedCredential("test-key", "test-secret"));
    }

    @Test
    void getAccountStatus_signsRequestAndMapsBalancesEquityAndBuyingPower() {
        server.expect(requestTo(BASE_URL + "/fapi/v3/account?timestamp=1785326400000&recvWindow=5000&signature=" + ACCOUNT_SIGNATURE))
                .andExpect(header("X-MBX-APIKEY", "test-key"))
                .andRespond(withSuccess(
                        "{\"totalMarginBalance\":\"126.72469206\",\"availableBalance\":\"100.00000000\","
                                + "\"assets\":[{\"asset\":\"USDT\",\"walletBalance\":\"126.72469206\",\"availableBalance\":\"100.00000000\"}]}",
                        MediaType.APPLICATION_JSON));

        BrokerAccountStatus status = adapter.getAccountStatus(TradingMode.PAPER);

        assertEquals(1, status.balances().size());
        assertEquals("USDT", status.balances().get(0).asset());
        assertEquals(0, new BigDecimal("100.00000000").compareTo(status.balances().get(0).free()));
        assertEquals(0, new BigDecimal("26.72469206").compareTo(status.balances().get(0).locked()));
        assertEquals(0, new BigDecimal("126.72469206").compareTo(status.equity()));
        assertEquals(0, new BigDecimal("100.00000000").compareTo(status.buyingPower()));
        server.verify();
    }

    @Test
    void getPosition_nonZeroAmount_signsRequestAndMapsResult() {
        server.expect(requestTo(BASE_URL + "/fapi/v3/positionRisk?symbol=BTCUSDT&timestamp=1785326400000&recvWindow=5000&signature=" + POSITION_SIGNATURE))
                .andExpect(header("X-MBX-APIKEY", "test-key"))
                .andRespond(withSuccess(
                        "[{\"symbol\":\"BTCUSDT\",\"positionAmt\":\"0.500\",\"entryPrice\":\"60000.00\",\"unRealizedProfit\":\"12.5\"}]",
                        MediaType.APPLICATION_JSON));

        Optional<BrokerPosition> position = adapter.getPosition("BTCUSDT", TradingMode.PAPER);

        assertTrue(position.isPresent());
        assertEquals(0, new BigDecimal("0.500").compareTo(position.get().quantity()));
        assertEquals(0, new BigDecimal("60000.00").compareTo(position.get().averageEntryPrice()));
        assertEquals(0, new BigDecimal("12.5").compareTo(position.get().unrealizedPnl()));
        assertEquals(AssetType.CRYPTO, position.get().assetType());
        server.verify();
    }

    @Test
    void getPosition_zeroPositionAmt_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/fapi/v3/positionRisk?symbol=BTCUSDT&timestamp=1785326400000&recvWindow=5000&signature=" + POSITION_SIGNATURE))
                .andRespond(withSuccess(
                        "[{\"symbol\":\"BTCUSDT\",\"positionAmt\":\"0\",\"entryPrice\":\"0\",\"unRealizedProfit\":\"0\"}]",
                        MediaType.APPLICATION_JSON));

        assertTrue(adapter.getPosition("BTCUSDT", TradingMode.PAPER).isEmpty());
    }

    @Test
    void getAccountStatus_rateLimited_throwsRateLimitedWithRetryAfter() {
        server.expect(requestTo(BASE_URL + "/fapi/v3/account?timestamp=1785326400000&recvWindow=5000&signature=" + ACCOUNT_SIGNATURE))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "15"));

        BrokerAdapterRateLimitedException ex = assertThrows(BrokerAdapterRateLimitedException.class,
                () -> adapter.getAccountStatus(TradingMode.PAPER));

        assertEquals(15L, ex.retryAfterSeconds());
    }

    @Test
    void getAccountStatus_ipAutoBanned_throwsRateLimited() {
        server.expect(requestTo(BASE_URL + "/fapi/v3/account?timestamp=1785326400000&recvWindow=5000&signature=" + ACCOUNT_SIGNATURE))
                .andRespond(withStatus(HttpStatus.I_AM_A_TEAPOT));

        BrokerAdapterRateLimitedException ex = assertThrows(BrokerAdapterRateLimitedException.class,
                () -> adapter.getAccountStatus(TradingMode.PAPER));

        assertNull(ex.retryAfterSeconds());
    }

    @Test
    void getAccountStatus_serverError_throwsTransientException() {
        server.expect(requestTo(BASE_URL + "/fapi/v3/account?timestamp=1785326400000&recvWindow=5000&signature=" + ACCOUNT_SIGNATURE))
                .andRespond(withServerError());

        assertThrows(BrokerAdapterTransientException.class, () -> adapter.getAccountStatus(TradingMode.PAPER));
    }

    @Test
    void getAccountStatus_invalidApiKey_throwsFatalException() {
        server.expect(requestTo(BASE_URL + "/fapi/v3/account?timestamp=1785326400000&recvWindow=5000&signature=" + ACCOUNT_SIGNATURE))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"code\":-2015,\"msg\":\"Invalid API-key, IP, or permissions for action.\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class, () -> adapter.getAccountStatus(TradingMode.PAPER));

        assertEquals(BrokerAdapterException.class, ex.getClass());
        assertTrue(ex.getMessage().contains("Invalid API-key"));
    }

    @Test
    void getAccountStatus_badSignature_throwsFatalException() {
        server.expect(requestTo(BASE_URL + "/fapi/v3/account?timestamp=1785326400000&recvWindow=5000&signature=" + ACCOUNT_SIGNATURE))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"code\":-1022,\"msg\":\"Signature for this request is not valid.\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class, () -> adapter.getAccountStatus(TradingMode.PAPER));

        assertEquals(BrokerAdapterException.class, ex.getClass());
        assertTrue(ex.getMessage().contains("Signature for this request is not valid"));
    }

    @Test
    void getAccountStatus_noCredentialConfigured_throwsWithoutCallingHttp() {
        when(credentialService.find(Broker.BINANCE, TradingMode.LIVE)).thenReturn(Optional.empty());

        assertThrows(BrokerAdapterException.class, () -> adapter.getAccountStatus(TradingMode.LIVE));
        server.verify();
    }

    // --- E4-F3-S2: placeOrder validation (fatal, pre-HTTP) ---

    @Test
    void placeOrder_stockAssetType_throwsWithoutCallingHttp() {
        BrokerOrderRequest request = sampleRequest(AssetType.STOCK, EntryOrderType.MARKET, BigDecimal.ONE);

        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class, () -> adapter.placeOrder(request, TradingMode.PAPER));

        assertTrue(ex.getMessage().contains("CRYPTO"));
        server.verify();
    }

    @Test
    void placeOrder_limitEntry_throwsWithoutCallingHttp() {
        BrokerOrderRequest request = sampleRequest(AssetType.CRYPTO, EntryOrderType.LIMIT, BigDecimal.ONE);

        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class, () -> adapter.placeOrder(request, TradingMode.PAPER));

        assertTrue(ex.getMessage().contains("MARKET"));
        server.verify();
    }

    @Test
    void placeOrder_leverageBelowOne_throwsWithoutCallingHttp() {
        BrokerOrderRequest request = sampleRequest(AssetType.CRYPTO, EntryOrderType.MARKET, BigDecimal.ZERO);

        assertThrows(BrokerAdapterException.class, () -> adapter.placeOrder(request, TradingMode.PAPER));
        server.verify();
    }

    @Test
    void placeOrder_leverageAboveMax_throwsWithoutCallingHttp() {
        BrokerOrderRequest request = sampleRequest(AssetType.CRYPTO, EntryOrderType.MARKET, new BigDecimal("21"));

        assertThrows(BrokerAdapterException.class, () -> adapter.placeOrder(request, TradingMode.PAPER));
        server.verify();
    }

    @Test
    void placeOrder_nonWholeLeverage_throwsWithoutCallingHttp() {
        BrokerOrderRequest request = sampleRequest(AssetType.CRYPTO, EntryOrderType.MARKET, new BigDecimal("2.5"));

        assertThrows(BrokerAdapterException.class, () -> adapter.placeOrder(request, TradingMode.PAPER));
        server.verify();
    }

    @Test
    void placeOrder_symbolWithQueryInjectionCharacters_throwsWithoutCallingHttp() {
        BrokerOrderRequest request = new BrokerOrderRequest(
                "co-1", "BTCUSDT&leverage=125", AssetType.CRYPTO, OrderSide.BUY, new BigDecimal("0.01"),
                EntryOrderType.MARKET, null, new BigDecimal("70000"), new BigDecimal("50000"), BigDecimal.ONE);

        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class, () -> adapter.placeOrder(request, TradingMode.PAPER));

        assertTrue(ex.getMessage().contains("Invalid symbol"));
        server.verify();
    }

    @Test
    void getOrderStatus_symbolWithQueryInjectionCharacters_throwsWithoutCallingHttp() {
        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class,
                () -> adapter.getOrderStatus("BTCUSDT&leverage=125", "co-1", TradingMode.PAPER));

        assertTrue(ex.getMessage().contains("Invalid symbol"));
        server.verify();
    }

    // --- E4-F3-S2: placeOrder happy path and partial-failure ---

    @Test
    void placeOrder_fullSuccess_setsLeveragePlacesEntryAndBothExitLegs() {
        BrokerOrderRequest request = sampleRequest(AssetType.CRYPTO, EntryOrderType.MARKET, new BigDecimal("5"));

        expectOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/leverage?")))
                .andExpect(queryParam("symbol", "BTCUSDT"))
                .andExpect(queryParam("leverage", "5"))
                .andRespond(withSuccess("{\"leverage\":5,\"maxNotionalValue\":\"1000000\",\"symbol\":\"BTCUSDT\"}", MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andExpect(queryParam("type", "MARKET"))
                .andExpect(queryParam("side", "BUY"))
                .andRespond(withSuccess(orderJson(555, "FILLED", "61000.00"), MediaType.APPLICATION_JSON));
        expectAlgoOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andExpect(queryParam("algoType", "CONDITIONAL"))
                .andExpect(queryParam("type", "STOP_MARKET"))
                .andExpect(queryParam("side", "SELL"))
                .andExpect(queryParam("closePosition", "true"))
                .andExpect(queryParam("triggerPrice", "50000"))
                .andRespond(withSuccess(algoOrderJson(556, "WORKING"), MediaType.APPLICATION_JSON));
        expectAlgoOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andExpect(queryParam("algoType", "CONDITIONAL"))
                .andExpect(queryParam("type", "TAKE_PROFIT_MARKET"))
                .andExpect(queryParam("side", "SELL"))
                .andExpect(queryParam("closePosition", "true"))
                .andExpect(queryParam("triggerPrice", "70000"))
                .andRespond(withSuccess(algoOrderJson(557, "WORKING"), MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(request, TradingMode.PAPER);

        assertEquals("co-full-success", result.clientOrderId());
        assertEquals("555", result.brokerOrderId());
        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(0, new BigDecimal("61000.00").compareTo(result.filledPrice()));
        assertNull(result.rejectionReason());
        server.verify();
    }

    @Test
    void placeOrder_highPrecisionQuantityAndPrices_truncatedToSymbolPrecisionBeforeSubmission() {
        // Mirrors what OrderService actually computes (amountUsd / price at scale 8) and
        // raw user-entered TP/SL prices — both routinely finer than Binance's own
        // per-symbol filters (BTCUSDT: quantityPrecision=3, pricePrecision=1), which
        // rejects the request outright rather than rounding it server-side.
        BrokerOrderRequest request = new BrokerOrderRequest(
                "co-precision", "BTCUSDT", AssetType.CRYPTO, OrderSide.SELL, new BigDecimal("0.0031086427"),
                EntryOrderType.MARKET, null, new BigDecimal("63000.126"), new BigDecimal("65500.987"), new BigDecimal("5"));

        expectOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/leverage?")))
                .andRespond(withSuccess("{\"leverage\":5,\"maxNotionalValue\":\"1000000\",\"symbol\":\"BTCUSDT\"}", MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andExpect(queryParam("type", "MARKET"))
                .andExpect(queryParam("quantity", "0.003"))
                .andRespond(withSuccess(orderJson(555, "FILLED", "64340.01"), MediaType.APPLICATION_JSON));
        expectAlgoOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andExpect(queryParam("type", "STOP_MARKET"))
                .andExpect(queryParam("triggerPrice", "65500.9"))
                .andRespond(withSuccess(algoOrderJson(556, "WORKING"), MediaType.APPLICATION_JSON));
        expectAlgoOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andExpect(queryParam("type", "TAKE_PROFIT_MARKET"))
                .andExpect(queryParam("triggerPrice", "63000.1"))
                .andRespond(withSuccess(algoOrderJson(557, "WORKING"), MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(request, TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, result.status());
        assertNull(result.rejectionReason());
        server.verify();
    }

    @Test
    void placeOrder_quantityRoundsToZeroAtSymbolPrecision_returnsRejectedWithoutPlacingEntry() {
        // DOGEUSDT's quantityPrecision is 0 (whole coins only) — 0.4 truncates to zero.
        BrokerOrderRequest request = new BrokerOrderRequest(
                "co-too-small", "DOGEUSDT", AssetType.CRYPTO, OrderSide.BUY, new BigDecimal("0.4"),
                EntryOrderType.MARKET, null, new BigDecimal("0.2"), new BigDecimal("0.05"), new BigDecimal("1"));

        expectOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/leverage?")))
                .andRespond(withSuccess("{\"leverage\":1,\"maxNotionalValue\":\"1000000\",\"symbol\":\"DOGEUSDT\"}", MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(request, TradingMode.PAPER);

        assertEquals(OrderStatus.REJECTED, result.status());
        assertTrue(result.rejectionReason().contains("rounds to zero"));
        server.verify();
    }

    @Test
    void placeOrder_leverageRejected_returnsRejectedResultWithoutPlacingEntry() {
        BrokerOrderRequest request = sampleRequest(AssetType.CRYPTO, EntryOrderType.MARKET, new BigDecimal("5"));

        expectOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/leverage?")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"code\":-4028,\"msg\":\"Leverage is not valid.\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(request, TradingMode.PAPER);

        assertEquals(OrderStatus.REJECTED, result.status());
        assertTrue(result.rejectionReason().contains("Leverage is not valid"));
        server.verify();
    }

    @Test
    void placeOrder_entryRejected_returnsRejectedResultWithoutPlacingExitLegs() {
        BrokerOrderRequest request = sampleRequest(AssetType.CRYPTO, EntryOrderType.MARKET, new BigDecimal("5"));

        expectOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/leverage?")))
                .andRespond(withSuccess("{\"leverage\":5,\"maxNotionalValue\":\"1000000\",\"symbol\":\"BTCUSDT\"}", MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"code\":-2019,\"msg\":\"Margin is insufficient.\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(request, TradingMode.PAPER);

        assertEquals(OrderStatus.REJECTED, result.status());
        assertTrue(result.rejectionReason().contains("Margin is insufficient"));
        server.verify();
    }

    @Test
    void placeOrder_takeProfitLegFailsAfterRetry_returnsPartiallyProtected() {
        BrokerOrderRequest request = sampleRequest(AssetType.CRYPTO, EntryOrderType.MARKET, new BigDecimal("5"));

        expectOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/leverage?")))
                .andRespond(withSuccess("{\"leverage\":5,\"maxNotionalValue\":\"1000000\",\"symbol\":\"BTCUSDT\"}", MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andExpect(queryParam("type", "MARKET"))
                .andRespond(withSuccess(orderJson(555, "FILLED", "61000.00"), MediaType.APPLICATION_JSON));
        expectAlgoOrderCheckNotFound(); // stop-loss check
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andExpect(queryParam("type", "STOP_MARKET"))
                .andRespond(withSuccess(algoOrderJson(556, "WORKING"), MediaType.APPLICATION_JSON));
        // take-profit: both bounded-retry attempts fail
        expectAlgoOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andExpect(queryParam("type", "TAKE_PROFIT_MARKET"))
                .andRespond(withServerError());
        expectAlgoOrderCheckNotFound();
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andExpect(queryParam("type", "TAKE_PROFIT_MARKET"))
                .andRespond(withServerError());

        BrokerOrderResult result = adapter.placeOrder(request, TradingMode.PAPER);

        assertEquals(OrderStatus.PARTIALLY_PROTECTED, result.status());
        assertEquals("555", result.brokerOrderId());
        assertEquals(0, new BigDecimal("61000.00").compareTo(result.filledPrice()));
        assertTrue(result.rejectionReason().contains("TAKE_PROFIT"));
        assertFalse(result.rejectionReason().contains("STOP_LOSS"));
        server.verify();
    }

    @Test
    void placeOrder_repeatedClientOrderId_replaysExistingLegsWithoutReposting() {
        BrokerOrderRequest request = sampleRequest(AssetType.CRYPTO, EntryOrderType.MARKET, new BigDecimal("5"));

        // Entry, stop-loss, and take-profit legs all already exist from a prior attempt.
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withSuccess(orderJson(555, "FILLED", "61000.00"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(556, "WORKING"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(557, "WORKING"), MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.placeOrder(request, TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals("555", result.brokerOrderId());
        assertNull(result.rejectionReason());
        server.verify(); // no POST calls at all — proves check-first idempotency
    }

    // --- E4-F3-S2: getOrderStatus ---

    @Test
    void getOrderStatus_unknownClientOrderId_returnsEmpty() {
        expectOrderCheckNotFound();

        assertTrue(adapter.getOrderStatus("BTCUSDT", "co-unknown", TradingMode.PAPER).isEmpty());
        server.verify();
    }

    @Test
    void getOrderStatus_entryFilledBothLegsResting_returnsFilled() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withSuccess(orderJson(555, "FILLED", "61000.00"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(556, "WORKING"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(557, "WORKING"), MediaType.APPLICATION_JSON));

        Optional<BrokerOrderResult> result = adapter.getOrderStatus("BTCUSDT", "co-1", TradingMode.PAPER);

        assertTrue(result.isPresent());
        assertEquals(OrderStatus.FILLED, result.get().status());
        server.verify();
    }

    @Test
    void getOrderStatus_missingExitLeg_returnsPartiallyProtected() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withSuccess(orderJson(555, "FILLED", "61000.00"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(556, "CANCELLED"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(557, "WORKING"), MediaType.APPLICATION_JSON));

        Optional<BrokerOrderResult> result = adapter.getOrderStatus("BTCUSDT", "co-1", TradingMode.PAPER);

        assertTrue(result.isPresent());
        assertEquals(OrderStatus.PARTIALLY_PROTECTED, result.get().status());
        server.verify();
    }

    @Test
    void getOrderStatus_exitLegTriggered_returnsCancelled() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withSuccess(orderJson(555, "FILLED", "61000.00"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(556, "FINISHED"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(557, "WORKING"), MediaType.APPLICATION_JSON));

        Optional<BrokerOrderResult> result = adapter.getOrderStatus("BTCUSDT", "co-1", TradingMode.PAPER);

        assertTrue(result.isPresent());
        assertEquals(OrderStatus.CANCELLED, result.get().status());
        server.verify();
    }

    @Test
    void getOrderStatus_exitLegNeverPlaced_returnsPartiallyProtected() {
        // Distinct from getOrderStatus_missingExitLeg_returnsPartiallyProtected (a leg that
        // exists but shows CANCELLED/EXPIRED/REJECTED): here the leg was never placed at all,
        // so findAlgoOrder's ALGO_ORDER_DOES_NOT_EXIST_CODE handling returns null.
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withSuccess(orderJson(555, "FILLED", "61000.00"), MediaType.APPLICATION_JSON));
        expectAlgoOrderCheckNotFound();
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(557, "WORKING"), MediaType.APPLICATION_JSON));

        Optional<BrokerOrderResult> result = adapter.getOrderStatus("BTCUSDT", "co-1", TradingMode.PAPER);

        assertTrue(result.isPresent());
        assertEquals(OrderStatus.PARTIALLY_PROTECTED, result.get().status());
        server.verify();
    }

    // --- E4-F3-S2: cancelOrder ---

    @Test
    void cancelOrder_unknownClientOrderId_returnsFailedWithoutThrowing() {
        expectOrderCheckNotFound();

        BrokerOrderResult result = adapter.cancelOrder("BTCUSDT", "co-unknown", TradingMode.PAPER);

        assertEquals(OrderStatus.FAILED, result.status());
        server.verify();
    }

    @Test
    void cancelOrder_alreadyFilledAndProtected_isIdempotentNoOpWithoutDeleting() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withSuccess(orderJson(555, "FILLED", "61000.00"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(556, "WORKING"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withSuccess(algoOrderJson(557, "WORKING"), MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.cancelOrder("BTCUSDT", "co-1", TradingMode.PAPER);

        assertEquals(OrderStatus.FILLED, result.status());
        server.verify(); // no DELETE call — idempotent no-op on a terminal composite status
    }

    @Test
    void cancelOrder_restingEntry_deletesThenReturnsAuthoritativeStatus() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withSuccess(orderJson(555, "NEW", "0"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.DELETE))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withSuccess(orderJson(555, "CANCELED", "0"), MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withSuccess(orderJson(555, "CANCELED", "0"), MediaType.APPLICATION_JSON));

        BrokerOrderResult result = adapter.cancelOrder("BTCUSDT", "co-1", TradingMode.PAPER);

        assertEquals(OrderStatus.CANCELLED, result.status());
        server.verify();
    }

    private BrokerOrderRequest sampleRequest(AssetType assetType, EntryOrderType entryOrderType, BigDecimal leverage) {
        return new BrokerOrderRequest(
                "co-full-success", "BTCUSDT", assetType, OrderSide.BUY, new BigDecimal("0.01"),
                entryOrderType, entryOrderType == EntryOrderType.LIMIT ? new BigDecimal("60000") : null,
                new BigDecimal("70000"), new BigDecimal("50000"), leverage);
    }

    private void expectOrderCheckNotFound() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/order?")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"code\":-2013,\"msg\":\"Order does not exist.\"}")
                        .contentType(MediaType.APPLICATION_JSON));
    }

    // E4-F3-S3: exit legs now check-first against the Algo Order API instead of /fapi/v1/order.
    private void expectAlgoOrderCheckNotFound() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith(BASE_URL + "/fapi/v1/algoOrder?")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"code\":-2013,\"msg\":\"Order does not exist.\"}")
                        .contentType(MediaType.APPLICATION_JSON));
    }

    private static String orderJson(long orderId, String status, String avgPrice) {
        return "{\"orderId\":" + orderId + ",\"clientOrderId\":\"ignored\",\"status\":\"" + status
                + "\",\"avgPrice\":\"" + avgPrice + "\"}";
    }

    private static String algoOrderJson(long algoId, String algoStatus) {
        return "{\"algoId\":" + algoId + ",\"algoStatus\":\"" + algoStatus + "\"}";
    }
}

package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
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

    @Test
    void placeOrder_notYetImplemented_throwsDeferredException() {
        BrokerOrderRequest request = new BrokerOrderRequest(
                "co-1", "BTCUSDT", AssetType.CRYPTO, OrderSide.BUY, new BigDecimal("0.01"),
                EntryOrderType.MARKET, null, new BigDecimal("70000"), new BigDecimal("50000"), BigDecimal.ONE);

        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class,
                () -> adapter.placeOrder(request, TradingMode.PAPER));

        assertTrue(ex.getMessage().contains("E4-F3-S2"));
        server.verify();
    }

    @Test
    void getOrderStatus_notYetImplemented_throwsDeferredException() {
        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class,
                () -> adapter.getOrderStatus("BTCUSDT", "co-1", TradingMode.PAPER));

        assertTrue(ex.getMessage().contains("E4-F3-S2"));
        server.verify();
    }

    @Test
    void cancelOrder_notYetImplemented_throwsDeferredException() {
        BrokerAdapterException ex = assertThrows(BrokerAdapterException.class,
                () -> adapter.cancelOrder("BTCUSDT", "co-1", TradingMode.PAPER));

        assertTrue(ex.getMessage().contains("E4-F3-S2"));
        server.verify();
    }
}

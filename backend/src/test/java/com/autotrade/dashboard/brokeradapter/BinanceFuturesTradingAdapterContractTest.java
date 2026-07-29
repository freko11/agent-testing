package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.lenient;

/**
 * Runs the shared {@link BrokerAdapterContractTest} suite against {@link
 * BinanceFuturesTradingAdapter} driven over a {@link
 * FakeBinanceFuturesTradingServer} — mirrors {@code
 * AlpacaTradingAdapterContractTest}'s template.
 *
 * <p><b>Intentionally disabled (confirmed with the user as this story's
 * scope):</b> {@code placeOrder}/{@code getOrderStatus}/{@code cancelOrder}
 * are not implemented until E4-F3-S2 (leverage/bracket-order design), so 5
 * of the 7 shared tests are {@code @Disabled} here rather than silently
 * skipped — this list is E4-F3-S2's checklist of exactly which tests to
 * re-enable once those methods are real. Only {@code
 * getAccountStatusReturnsNonEmptyBalances} and {@code
 * getPositionForASymbolWithNoActivityReturnsEmpty} run for real, per {@link
 * BrokerAdapterContractTest}'s own documented allowance for a real-adapter
 * subclass to skip assertions that don't apply.
 */
@ExtendWith(MockitoExtension.class)
class BinanceFuturesTradingAdapterContractTest extends BrokerAdapterContractTest {

    @Mock
    private BrokerCredentialService credentialService;

    private BinanceFuturesTradingAdapter adapter;

    @BeforeEach
    void setUpAdapter() {
        FakeBinanceFuturesTradingServer fakeServer = new FakeBinanceFuturesTradingServer();
        RestClient restClient = fakeServer.buildRestClient("https://testnet.binancefuture.com");
        adapter = new BinanceFuturesTradingAdapter(Map.of(TradingMode.PAPER, restClient), credentialService, Clock.systemUTC());

        BrokerCredential stored = new BrokerCredential(Broker.BINANCE, TradingMode.PAPER, "ignored", "ignored");
        lenient().when(credentialService.find(Broker.BINANCE, TradingMode.PAPER)).thenReturn(Optional.of(stored));
        lenient().when(credentialService.readDecrypted(stored))
                .thenReturn(new BrokerCredentialService.DecryptedCredential("test-key", "test-secret"));
    }

    @Override
    protected BrokerAdapter adapter() {
        return adapter;
    }

    @Override
    protected TradingMode tradingMode() {
        return TradingMode.PAPER;
    }

    @Override
    protected String tradableSymbol() {
        return "BTCUSDT";
    }

    @Override
    protected BrokerOrderRequest sampleBuyOrderRequest(String clientOrderId) {
        return new BrokerOrderRequest(
                clientOrderId, tradableSymbol(), AssetType.CRYPTO, OrderSide.BUY, new BigDecimal("0.01"),
                EntryOrderType.MARKET, null, new BigDecimal("70000"), new BigDecimal("50000"), BigDecimal.ONE);
    }

    @Test
    @Disabled("placeOrder deferred to E4-F3-S2 — Binance Futures leverage/bracket-order design not done yet")
    @Override
    void placeOrderReturnsResultMatchingRequestedClientOrderId() {
    }

    @Test
    @Disabled("placeOrder deferred to E4-F3-S2 — Binance Futures leverage/bracket-order design not done yet")
    @Override
    void placeOrderIsIdempotentForARepeatedClientOrderId() {
    }

    @Test
    @Disabled("getOrderStatus deferred to E4-F3-S2")
    @Override
    void getOrderStatusForAnUnknownClientOrderIdReturnsEmpty() {
    }

    @Test
    @Disabled("cancelOrder deferred to E4-F3-S2")
    @Override
    void cancelOrderOnAnOpenOrderSucceedsWithoutThrowing() {
    }

    @Test
    @Disabled("cancelOrder deferred to E4-F3-S2")
    @Override
    void cancelOrderOnAnUnknownClientOrderIdReturnsAResultRatherThanThrowing() {
    }
}

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
 * Proves {@link RetryingBrokerAdapter} is fully transparent on the happy
 * path when wrapping the real {@link BinanceFuturesTradingAdapter},
 * following the "wrap your adapter and run the shared suite again" template
 * {@code RetryingMockBrokerAdapterContractTest}/{@code
 * RetryingAlpacaTradingAdapterContractTest} already established. Same
 * disabled-test subset as {@link BinanceFuturesTradingAdapterContractTest},
 * for the same E4-F3-S2-deferred-scope reason.
 */
@ExtendWith(MockitoExtension.class)
class RetryingBinanceFuturesTradingAdapterContractTest extends BrokerAdapterContractTest {

    @Mock
    private BrokerCredentialService credentialService;

    private RetryingBrokerAdapter adapter;

    @BeforeEach
    void setUpAdapter() {
        FakeBinanceFuturesTradingServer fakeServer = new FakeBinanceFuturesTradingServer();
        RestClient restClient = fakeServer.buildRestClient("https://testnet.binancefuture.com");
        BinanceFuturesTradingAdapter delegate =
                new BinanceFuturesTradingAdapter(Map.of(TradingMode.PAPER, restClient), credentialService, Clock.systemUTC());
        adapter = new RetryingBrokerAdapter(delegate);

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

package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.BeforeEach;
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
 * AlpacaTradingAdapterContractTest}'s template. All 7 shared tests run for
 * real now that {@code placeOrder}/{@code getOrderStatus}/{@code
 * cancelOrder} are implemented (E4-F3-S2) — previously 5 were {@code
 * @Disabled} pending this story.
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
}

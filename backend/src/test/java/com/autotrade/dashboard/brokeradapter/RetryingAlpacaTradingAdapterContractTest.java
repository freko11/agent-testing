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
 * Proves {@link RetryingBrokerAdapter} is fully transparent on the happy
 * path when wrapping the real {@link AlpacaTradingAdapter}, following the
 * "wrap your adapter and run the shared suite again" template {@code
 * RetryingMockBrokerAdapterContractTest} already established for the mock.
 */
@ExtendWith(MockitoExtension.class)
class RetryingAlpacaTradingAdapterContractTest extends BrokerAdapterContractTest {

    @Mock
    private BrokerCredentialService credentialService;

    private RetryingBrokerAdapter adapter;

    @BeforeEach
    void setUpAdapter() {
        FakeAlpacaTradingServer fakeServer = new FakeAlpacaTradingServer();
        RestClient restClient = fakeServer.buildRestClient("https://paper-api.alpaca.markets");
        AlpacaTradingAdapter delegate =
                new AlpacaTradingAdapter(Map.of(TradingMode.PAPER, restClient), credentialService, Clock.systemUTC());
        adapter = new RetryingBrokerAdapter(delegate);

        BrokerCredential stored = new BrokerCredential(Broker.ALPACA, TradingMode.PAPER, "ignored", "ignored");
        lenient().when(credentialService.find(Broker.ALPACA, TradingMode.PAPER)).thenReturn(Optional.of(stored));
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
        return "AAPL";
    }

    @Override
    protected BrokerOrderRequest sampleBuyOrderRequest(String clientOrderId) {
        return new BrokerOrderRequest(
                clientOrderId, tradableSymbol(), AssetType.STOCK, OrderSide.BUY, new BigDecimal("10"),
                EntryOrderType.MARKET, null, new BigDecimal("220"), new BigDecimal("180"), BigDecimal.ONE);
    }
}

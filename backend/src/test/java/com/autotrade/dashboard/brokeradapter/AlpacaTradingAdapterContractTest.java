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
 * AlpacaTradingAdapter} driven over a {@link FakeAlpacaTradingServer} —
 * proves the real adapter satisfies the same plumbing contract {@code
 * MockBrokerAdapterContractTest} proves for the test-only mock, through real
 * HTTP request/response shapes rather than direct in-memory state.
 */
@ExtendWith(MockitoExtension.class)
class AlpacaTradingAdapterContractTest extends BrokerAdapterContractTest {

    @Mock
    private BrokerCredentialService credentialService;

    private AlpacaTradingAdapter adapter;

    @BeforeEach
    void setUpAdapter() {
        FakeAlpacaTradingServer fakeServer = new FakeAlpacaTradingServer();
        RestClient restClient = fakeServer.buildRestClient("https://paper-api.alpaca.markets");
        adapter = new AlpacaTradingAdapter(Map.of(TradingMode.PAPER, restClient), credentialService, Clock.systemUTC());

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

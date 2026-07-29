package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.ticker.AssetType;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Proves {@link RetryingBrokerAdapter} is fully transparent on the happy
 * path (no scripted failures) by running the shared contract suite against
 * a {@link MockBrokerAdapter} wrapped in it — the template a future F4.2/F4.3
 * adapter can follow to run the same suite against its own wrapped instance.
 */
class RetryingMockBrokerAdapterContractTest extends BrokerAdapterContractTest {

    private final RetryingBrokerAdapter adapter =
            new RetryingBrokerAdapter(new MockBrokerAdapter(Broker.ALPACA, AssetType.STOCK, true, Clock.systemUTC()));

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
                clientOrderId,
                tradableSymbol(),
                AssetType.STOCK,
                OrderSide.BUY,
                new BigDecimal("10"),
                EntryOrderType.MARKET,
                null,
                new BigDecimal("220"),
                new BigDecimal("180"),
                BigDecimal.ONE);
    }
}

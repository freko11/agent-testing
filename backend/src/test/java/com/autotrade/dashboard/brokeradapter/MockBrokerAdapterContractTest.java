package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.ticker.AssetType;

import java.math.BigDecimal;
import java.time.Clock;

/** Proves {@link BrokerAdapterContractTest}'s shared suite runs against {@link MockBrokerAdapter}. */
class MockBrokerAdapterContractTest extends BrokerAdapterContractTest {

    private final MockBrokerAdapter adapter = new MockBrokerAdapter(Broker.ALPACA, AssetType.STOCK, true, Clock.systemUTC());

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

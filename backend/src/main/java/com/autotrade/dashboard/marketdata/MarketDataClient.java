package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.ticker.AssetType;

import java.util.List;

/**
 * A source of historical candle data for one asset type. Deliberately
 * separate from E4's future {@code BrokerAdapter} (placeOrder/getPosition/
 * etc.) — reading public candles and placing money-moving orders are
 * different enough concerns (no idempotency/order-state semantics here)
 * that sharing one interface would either bloat it or weaken it.
 */
public interface MarketDataClient {

    AssetType supportedAssetType();

    Broker broker();

    List<Candle> fetchRecentCandles(String symbol, int limit);
}

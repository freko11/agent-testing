package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.ticker.AssetType;

import java.util.Optional;

/**
 * A trading interface for one broker/exchange (placeOrder, getOrderStatus,
 * getPosition, cancelOrder, getAccountStatus) so trading logic never talks
 * to a broker's raw API directly. Deliberately separate from {@code
 * com.autotrade.dashboard.marketdata.MarketDataClient} — reading public
 * candles and placing money-moving orders don't share enough concerns to
 * justify one interface.
 *
 * <p>Every method takes an explicit {@link TradingMode} so a paper/live
 * switch (E6.1) changes which credentials/base URL an adapter uses without
 * re-instantiating anything. Expected business outcomes (broker rejected an
 * order, an order/position wasn't found) are returned as normal result
 * values, never exceptions; only transport/infrastructure faults throw
 * {@link BrokerAdapterException}. Retry/backoff/rate-limit handling (E4-F1-S2)
 * and outage/duplicate-prevention semantics (E4-F1-S3) are deliberately not
 * part of this contract yet — {@code clientOrderId} being the sole required
 * identifier everywhere, and {@link BrokerAdapterException} being the single
 * seam a future retry wrapper would catch, are what keep this interface from
 * needing a breaking change once those stories land.
 */
public interface BrokerAdapter {

    AssetType supportedAssetType();

    Broker broker();

    BrokerOrderResult placeOrder(BrokerOrderRequest request, TradingMode mode);

    Optional<BrokerOrderResult> getOrderStatus(String clientOrderId, TradingMode mode);

    Optional<BrokerPosition> getPosition(String symbol, TradingMode mode);

    BrokerOrderResult cancelOrder(String clientOrderId, TradingMode mode);

    BrokerAccountStatus getAccountStatus(TradingMode mode);
}

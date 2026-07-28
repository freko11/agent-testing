package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.ticker.AssetType;

import java.math.BigDecimal;

/**
 * A bracket-order request (entry + take-profit + stop-loss) to submit via a
 * {@link BrokerAdapter}. {@code clientOrderId} is the app-generated
 * idempotency key (see {@code Order}'s own Javadoc) — callers must never
 * regenerate it on retry. {@code entryLimitPrice} is non-null iff {@code
 * entryOrderType == LIMIT}, else null.
 */
public record BrokerOrderRequest(
        String clientOrderId,
        String symbol,
        AssetType assetType,
        OrderSide side,
        BigDecimal quantity,
        EntryOrderType entryOrderType,
        BigDecimal entryLimitPrice,
        BigDecimal takeProfitPrice,
        BigDecimal stopLossPrice,
        BigDecimal leverage) {
}

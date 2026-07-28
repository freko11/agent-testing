package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.ticker.AssetType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An account-level net holding for one symbol, as reported by a {@link
 * BrokerAdapter#getPosition}. {@code unrealizedPnl} is nullable — not every
 * broker always computes it.
 */
public record BrokerPosition(
        String symbol,
        AssetType assetType,
        BigDecimal quantity,
        BigDecimal averageEntryPrice,
        BigDecimal unrealizedPnl,
        Instant asOf) {
}

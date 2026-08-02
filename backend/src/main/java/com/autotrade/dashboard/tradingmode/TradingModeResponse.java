package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.common.TradingMode;

import java.time.Instant;

/**
 * The current global trading mode plus when it was last explicitly changed (null if never), and progress
 * toward the paper-trade threshold that gates LIVE mode (E6-F1-S2) — returned on every read so the frontend
 * can proactively disable the LIVE switch instead of only reacting to a failed attempt.
 */
public record TradingModeResponse(
        TradingMode mode,
        Instant changedAt,
        long successfulPaperTrades,
        int paperTradeThreshold,
        boolean liveModeUnlocked) {
}

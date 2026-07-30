package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.common.TradingMode;

import java.time.Instant;

/** The current global trading mode plus when it was last explicitly changed (null if never). */
public record TradingModeResponse(TradingMode mode, Instant changedAt) {
}

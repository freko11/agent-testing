package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.signal.HoldTerm;
import com.autotrade.dashboard.signal.SignalRuleId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One walk-forward replay step: the indicator snapshot and matched rule as of a single
 * historical candle. Kept only for BUY/SELL calls, for the printed report's per-day
 * spot-check table — HOLD rows are summarized in aggregate ({@link HoldGateStats}), not
 * dumped per-day, to keep the report readable across ~1000 candles of history.
 */
public record BacktestDecisionPoint(int index, Instant date, BigDecimal rsi, BigDecimal macdHistogram,
                                     BigDecimal volatility, BigDecimal volumeTrend, SignalRuleId matchedRule,
                                     HoldTerm holdTerm) {
}

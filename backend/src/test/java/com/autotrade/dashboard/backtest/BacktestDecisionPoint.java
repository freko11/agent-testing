package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.signal.HoldTerm;
import com.autotrade.dashboard.signal.SignalRuleId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * One walk-forward replay step: the indicator snapshot and matched rule as of a single
 * historical candle. Kept only for BUY/SELL calls, for the printed report's per-day
 * spot-check table — HOLD rows are summarized in aggregate ({@link HoldGateStats}), not
 * dumped per-day, to keep the report readable across ~1000 candles of history.
 *
 * <p>{@code minResult}/{@code midResult}/{@code maxResult} (E8-F2-S1) are the same three
 * per-checkpoint {@link DirectionalScoreResult}s already accumulated into this rule's aggregate
 * stats, threaded through here too so the spot-check table can show exit reason at decision-point
 * granularity, not just in aggregate. Empty exactly when the fixture doesn't reach that
 * checkpoint's horizon.
 */
public record BacktestDecisionPoint(int index, Instant date, BigDecimal rsi, BigDecimal macdHistogram,
                                     BigDecimal volatility, BigDecimal volumeTrend, SignalRuleId matchedRule,
                                     HoldTerm holdTerm, Optional<DirectionalScoreResult> minResult,
                                     Optional<DirectionalScoreResult> midResult,
                                     Optional<DirectionalScoreResult> maxResult) {
}

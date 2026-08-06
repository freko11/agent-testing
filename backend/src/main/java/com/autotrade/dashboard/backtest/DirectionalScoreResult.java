package com.autotrade.dashboard.backtest;

import java.math.BigDecimal;

/**
 * One {@link Checkpoint}'s scored outcome for a BUY/SELL decision point: the WIN/LOSS/WASH
 * classification (E2-F4-S1) plus the actual signed forward return (E2-F4-S2) that classification
 * was derived from — carried alongside the outcome so {@link CheckpointStats} can report
 * per-branch expectancy (average win size vs. average loss size), not just hit rate. {@code
 * exitReason} (E8-F2-S1) records whether this was resolved by an intraday TP/SL crossing before
 * this checkpoint's day, or by the existing fixed-day endpoint fallback.
 *
 * <p>Promoted to main scope (E8-F5-S1) — see {@link BacktestConfig}'s class Javadoc.
 */
public record DirectionalScoreResult(DirectionalOutcome outcome, BigDecimal signedReturnPct, ExitReason exitReason) {
}
